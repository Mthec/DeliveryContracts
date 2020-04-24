package mod.wurmunlimited.delivery;

import com.wurmonline.server.*;
import com.wurmonline.server.behaviours.*;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.MonetaryConstants;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.items.*;
import com.wurmonline.server.players.Player;
import com.wurmonline.shared.constants.IconConstants;
import com.wurmonline.shared.constants.ItemMaterials;
import javassist.NotFoundException;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.*;
import org.gotti.wurmunlimited.modsupport.ItemTemplateBuilder;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DeliveryContractsMod implements WurmServerMod, Configurable, PreInitable, Initable, ItemTemplatesCreatedListener, ServerStartedListener, PlayerMessageListener {
    private static final Logger logger = Logger.getLogger(DeliveryContractsMod.class.getName());
    private static int templateId;
    private int contractPrice = MonetaryConstants.COIN_COPPER * 10;
    private boolean updateTraders = false;
    private boolean contractsOnTraders = true;
    private int itemCap = 1000;
    private short packActionId;
    private static final Map<Creature, Integer> weightBlocker = new HashMap<>();
    public static boolean setNoDecay;
    public static boolean setNoDecayFood;
    private long lastCleanup;

    public static final Set<Item> blockWeight = new HashSet<>();

    // The following would be nice, but would require big workaround that is arguably not worth the effort for marginal benefit.
    // Get Price after sale from trader is modified from full price.  (Unnecessary ItemBehaviour.action override with edge cases.)

    public static void addWeightToBlock(Creature creature, int weight) {
        weightBlocker.merge(creature, weight, Integer::sum);
    }

    public static void removeWeightToBlock(Creature creature, int weight) {
        weightBlocker.merge(creature, -weight, Integer::sum);
        if (weightBlocker.get(creature) <= 0)
            weightBlocker.remove(creature);
    }

    @Override
    public void configure(Properties properties) {
        String val = properties.getProperty("contract_price_in_irons");
        if (val != null && val.length() > 0) {
            try {
                int newPrice = Integer.parseInt(val);
                if (newPrice < 0)
                    throw new NumberFormatException();
                contractPrice = newPrice;
            } catch (NumberFormatException e) {
                logger.warning("Invalid value for contract_price_in_irons.  Must be a non-negative whole number.");
            }
        }
        val = properties.getProperty("update_traders");
        if (val != null && val.equals("true"))
            updateTraders = true;
        val = properties.getProperty("contracts_on_traders");
        if (val != null && val.equals("false"))
            contractsOnTraders = false;
        val = properties.getProperty("max_items");
        if (val != null && val.length() > 0) {
            try {
                int maxItems = Integer.parseInt(val);
                if (maxItems < 0)
                    throw new NumberFormatException();
                itemCap = maxItems;
            } catch (NumberFormatException e) {
                logger.warning("Invalid value for max_items.  Must be a non-negative whole number.  Using default of 1000.");
            }
        }
        val = properties.getProperty("no_decay_in_contract");
        if (val != null && val.equals("true"))
            setNoDecay = true;
        val = properties.getProperty("no_decay_food");
        if (val != null && val.equals("true"))
            setNoDecayFood = true;
    }

    public static int getTemplateId() {
        return templateId;
    }

    @Override
    public void onItemTemplatesCreated() {
        try {
            ItemTemplate template = new ItemTemplateBuilder("contract.delivery")
                            .name("delivery contract", "delivery contracts", "A contract to call on the spirits to deliver items.  Use it to sell bulk items on a merchant and have them delivered to the holders chosen destination on arrival.")
                            .modelName("model.writ.")
                            .imageNumber((short)IconConstants.ICON_SCROLL_TEXT)
                            .weightGrams(0)
                            .dimensions(1, 10, 10)
                            .decayTime(Long.MAX_VALUE)
                            .material(ItemMaterials.MATERIAL_PAPER)
                            .behaviourType(BehaviourList.itemBehaviour)
                            .itemTypes(new short[] {
                                    ItemTypes.ITEM_TYPE_FULLPRICE,
                                    ItemTypes.ITEM_TYPE_INDESTRUCTIBLE,
                                    ItemTypes.ITEM_TYPE_LOADED,
                                    ItemTypes.ITEM_TYPE_NOT_MISSION,
                                    ItemTypes.ITEM_TYPE_HOLLOW,
                                    ItemTypes.ITEM_TYPE_NOSELLBACK
                            })
                            .value(contractPrice)
                            .difficulty(100.0F)
                            .build();
            templateId = template.getTemplateId();

            if (template.isPurchased()) {
                throw new RuntimeException();
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onServerStarted() {
        PackContractAction packAction = new PackContractAction();
        packActionId = packAction.getActionId();
        ModActions.registerAction(packAction);
        ModActions.registerAction(new DeliverAction());

        try {
            Field field = ReflectionUtil.getField(Class.forName("com.wurmonline.server.creatures.BuyerHandler"), "deliveryContractId");
            field.setAccessible(true);
            field.set(null, templateId);
        } catch (ClassNotFoundException ignored) {
        } catch (IllegalAccessException | NoSuchFieldException e) {
            logger.warning("Error setting template id for Buyer Merchant mod, contracts will not be processed by buyers.");
            e.printStackTrace();
        }

        PackContractAction.itemCap = itemCap;

        if (updateTraders) {
            if (contractsOnTraders) {
                for (Shop shop : Economy.getTraders()) {
                    Creature creature = Creatures.getInstance().getCreatureOrNull(shop.getWurmId());
                    if (!shop.isPersonal() && creature != null && creature.isSalesman() && creature.getInventory().getItems().stream().noneMatch(i -> i.getTemplateId() == templateId)) {
                        try {
                            creature.getInventory().insertItem(Creature.createItem(templateId, (float) (10 + Server.rand.nextInt(80))));
                            shop.setMerchantData(shop.getNumberOfItems() + 1);
                        } catch (Exception e) {
                            logger.log(Level.INFO, "Failed to create trader inventory items for shop, creature: " + creature.getName(), e);
                        }
                    }
                }
            } else {
                for (Shop shop : Economy.getTraders()) {
                    Creature creature = Creatures.getInstance().getCreatureOrNull(shop.getWurmId());
                    if (!shop.isPersonal() && creature != null) {
                        creature.getInventory().getItems().stream().filter(i -> i.getTemplateId() == templateId).collect(Collectors.toList()).forEach(item -> {
                            Items.destroyItem(item.getWurmId());
                            shop.setMerchantData(shop.getNumberOfItems() - 1);
                        });
                    }
                }
            }
        }
    }

    @Override
    public void preInit() {
        ModActions.init();
    }

    @Override
    public void init() {
        HookManager manager = HookManager.getInstance();

        // Add delivery contracts to traders default inventory.
        manager.registerHook("com.wurmonline.server.economy.Shop",
                "createShop",
                "(Lcom/wurmonline/server/creatures/Creature;)V",
                () -> this::createShop);

        manager.registerHook("com.wurmonline.server.items.Item",
                "isEmpty",
                "(Z)Z",
                () -> this::isEmpty);

        // These two can cause problems when trading.  Items will only be accessible through getItems() during trade.
        manager.registerHook("com.wurmonline.server.items.Item",
                "getItemsAsArray",
                "()[Lcom/wurmonline/server/items/Item;",
                () -> this::getItemsAsArray);

        manager.registerHook("com.wurmonline.server.items.Item",
                "getAllItems",
                "(ZZ)[Lcom/wurmonline/server/items/Item;",
                () -> this::getItemsAsArray);

        // Hide weight of items in contracts from creature speed modifier.
        manager.registerHook("com.wurmonline.server.creatures.Creature",
                "addCarriedWeight",
                "(I)V",
                () -> this::addCarriedWeight);

        manager.registerHook("com.wurmonline.server.creatures.Creature",
                "removeCarriedWeight",
                "(I)Z",
                () -> this::removeCarriedWeight);

        manager.registerHook("com.wurmonline.server.items.Item",
                "setOwner",
                "(JJZ)V",
                () -> this::setOwner);

        manager.registerHook("com.wurmonline.server.items.DbItem",
                "setOwnerStuff",
                "(Lcom/wurmonline/server/items/ItemTemplate;)V",
                () -> this::setOwner);

        // To prevent extracting items from contract.
        manager.registerHook("com.wurmonline.server.items.Item",
                "moveToItem",
                "(Lcom/wurmonline/server/creatures/Creature;JZ)Z",
                () -> this::moveToItem);

        manager.registerHook("com.wurmonline.server.items.TradingWindow",
                "mayAddFromInventory",
                "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Z",
                () -> this::mayAddFromInventory);

        // Full price on Trader, not full price on merchant.
        manager.registerHook("com.wurmonline.server.items.Item",
                "isFullprice",
                "()Z",
                () -> this::isFullPrice);

        // Block actions whilst item is inside a contract.
        manager.registerHook("com.wurmonline.server.behaviours.ItemBehaviour",
                "getBehavioursFor",
                "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Ljava/util/List;",
                () -> this::getBehavioursFor);

        manager.registerHook("com.wurmonline.server.behaviours.ItemBehaviour",
                "getBehavioursFor",
                "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/items/Item;)Ljava/util/List;",
                () -> this::getBehavioursFor);

        // Override to prevent blocking inserting items into inventory due to weight.
        manager.registerHook("com.wurmonline.server.items.Item",
                "getFullWeight",
                "()I",
                () -> this::getFullWeight);

        manager.registerHook("com.wurmonline.server.items.Item",
                "getFullWeight",
                "(Z)I",
                () -> this::getFullWeight);

        // Fix multiple items being sent to pack action when they are in an inventory window.
        manager.registerHook("com.wurmonline.server.behaviours.BehaviourDispatcher",
                "action",
                "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/creatures/Communicator;JJS)V",
                () -> this::behaviourDispatcher);

        // Destroy contents of contract when contract is destroyed.
        manager.registerHook("com.wurmonline.server.Items",
                "destroyItem",
                "(JZZ)V",
                () -> this::destroyItem);


        manager.registerHook("com.wurmonline.server.items.Item",
                "getWeightGrams",
                "()I",
                () -> this::getWeightGrams);

        manager.registerHook("com.wurmonline.server.items.Item",
                "getWeightGrams",
                "(Z)I",
                () -> this::getWeightGrams);

        manager.registerHook("com.wurmonline.server.items.DbItem",
                "getWeightGrams",
                "()I",
                () -> this::getWeightGrams);

        manager.registerHook("com.wurmonline.server.creatures.Communicator",
                "reallyHandle_CMD_MOVE_INVENTORY",
                "(Ljava/nio/ByteBuffer;)V",
                () -> this::reallyHandle_MOVE);

        manager.registerHook("com.wurmonline.server.behaviours.MethodsItems",
                "drop",
                "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Z)[Ljava/lang/String;",
                () -> this::drop);

        manager.registerHook("com.wurmonline.server.behaviours.MethodsItems",
                "placeItem",
                "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;Lcom/wurmonline/server/behaviours/Action;F)Z",
                () -> this::placeItem);

        manager.registerHook("com.wurmonline.server.items.Item",
                "isNoTake",
                "()Z",
                () -> this::isNoTake);

        try {
            manager.getClassPool().getCtClass("com.wurmonline.server.items.BuyerTradingWindow");
            manager.registerHook("com.wurmonline.server.items.BuyerTradingWindow",
                    "mayAddFromInventory",
                    "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Z",
                    () -> this::mayAddFromInventory);
            logger.info("Added hook to Buyer Merchant successfully.");
        } catch (NotFoundException ignored) {}
    }

    private Object reallyHandle_MOVE(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        ByteBuffer byteBuffer = ((ByteBuffer)args[0]).duplicate();
        final int nums = byteBuffer.getShort() & 0xFFFF;
        long[] subjectIds = new long[nums];
        for (int x = 0; x < nums; ++x) {
            subjectIds[x] = byteBuffer.getLong();
        }
        long targetId = byteBuffer.getLong();

        try {
            for (long id : subjectIds) {
                Item item = Items.getItem(id);
                if (item.getTemplateId() == templateId && !item.isTraded()) {
                    Item from = item.getTopParentOrNull();
                    Item to = Items.getItem(targetId).getTopParentOrNull();

                    if (from != null && to != null) {
                        if (!((from.isInventory() && to.isMailBox()) || to.isInventory())) {
                            ((Communicator)o).sendSafeServerMessage("You may not drop " + (subjectIds.length == 1 ? "that item" : " at least some of those items") + ".");
                            return null;
                        }
                    }
                }
            }
        } catch (NoSuchItemException e) {
            logger.warning("Not an item.  This may be okay.");
            e.printStackTrace();
        }

        return method.invoke(o, args);
    }

    private boolean canDrop(Creature creature, Item item) {
        if (item.getTemplateId() == templateId) {
            creature.getCommunicator().sendSafeServerMessage("You are not allowed to drop that.");
            return false;
        }

        return true;
    }

    private Object drop(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        if (!canDrop((Creature)args[0], (Item)args[1])) {
            return new String[0];
        }
        return method.invoke(o, args);
    }

    private Object placeItem(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        if (!canDrop((Creature)args[0], (Item)args[1])) {
            return true;
        }
        return method.invoke(o, args);
    }

    private Object isNoTake(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        if (isInContract((Item)o))
            return false;
        return method.invoke(o, args);
    }

    @Override
    public MessagePolicy onPlayerMessage(Communicator communicator, String message, String title) {
        Player player = communicator.getPlayer();

        if (player != null && player.getPower() >= 2 && message.equals("#dcCleanup")) {
            if (System.currentTimeMillis() - lastCleanup > TimeConstants.MINUTE_MILLIS) {
                int count = 0;
                for (Item item : Items.getAllItems()) {
                    if (item.mailed && !WurmMail.isItemInMail(item.getWurmId()) && !isInContract(item)) {
                        Items.destroyItem(item.getWurmId());
                        ++count;
                    }
                }

                lastCleanup = System.currentTimeMillis();
                String returnMessage = "Cleaned up " + count + " items.";
                player.getCommunicator().sendSafeServerMessage(returnMessage);
                logger.info(returnMessage);
                return MessagePolicy.DISCARD;
            }
            player.getCommunicator().sendSafeServerMessage("You must wait 1 minute before using that command again.");
            return MessagePolicy.DISCARD;
        }

        return MessagePolicy.PASS;
    }

    @Override
    public boolean onPlayerMessage(Communicator communicator, String message) {
        return false;
    }

    Object destroyItem(Object o, Method method, Object[] args) throws Throwable {
        try {
            Item contractOrContents = Items.getItem((Long)args[0]);
            if (contractOrContents.getTemplateId() == templateId || isInContract(contractOrContents)) {
                for (Item item : contractOrContents.getItems().toArray(new Item[0])) {
                    // Not sure if it is needed for recycled items.
                    item.setMailed(false);
                    Items.destroyItem(item.getWurmId());
                }
            }
        } catch (NoSuchItemException ignored) {}

        return method.invoke(o, args);
    }

    Object behaviourDispatcher(Object o, Method method, Object[] args) throws Throwable {
        Short action = (Short)args[4];
        if (action == packActionId) {
            try {
                Item contract = Items.getItem((Long)args[2]);
                Item target = Items.getItem((Long)args[3]);

                if (contract.getItems().contains(target))
                    return null;
            } catch (NoSuchItemException ignored) {}
        } else if (action == Actions.DESTROY) {
            try {
                Creature creature = (Creature)args[0];
                if (creature.getPower() >= 2) {
                    Item target = Items.getItem((Long)args[3]);
                    if (target.isMailed() && !WurmMail.isItemInMail(target.getWurmId())) {
                        target.setMailed(false);
                    }
                }
            } catch (NoSuchItemException ignored) {}
        }

        try {
            return method.invoke(o, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    Object isFullPrice(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Item item = (Item)o;
        if (item.getTemplateId() == templateId && item.getItemCount() > 0)
            return false;

        return method.invoke(o, args);
    }

    private boolean isInContract(Item item) {
        Item maybeContract = item.getParentOrNull();

        if (maybeContract != null) {
            while (true) {
                Item parent = maybeContract.getParentOrNull();
                if (parent == null || parent.getTemplateId() == ItemList.inventory || parent.isMailBox()) {
                    break;
                }
                maybeContract = parent;
            }
        }

        return maybeContract != null && maybeContract.getTemplateId() == templateId;
    }

    Object mayAddFromInventory(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        if (isInContract((Item)args[1]))
            return false;
        return method.invoke(o, args);
    }
    
    Object moveToItem(Object o, Method method, Object[] args) throws Throwable {
        try {
            if (isInContract((Item)o) || isInContract(Items.getItem((long)args[1])))
                return false;
        } catch (NoSuchItemException ignored) {}

        try {
            return method.invoke(o, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    Object setOwner(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        try {
            Item item = (Item)o;
            if (isInContract(item)) {
                Creature owner = null;
                if (args.length == 3) {
                    try {
                        owner = Server.getInstance().getCreature((Long)args[0]);
                    } catch (NoSuchPlayerException | NoSuchCreatureException ignored) {}
                }

                if (owner == null)
                    owner = Server.getInstance().getCreature(item.getOwnerId());

                weightBlocker.put(owner, item.getWeightGrams());
                method.invoke(o, args);
                weightBlocker.remove(owner);

                return null;
            }
        } catch (NoSuchPlayerException | NoSuchCreatureException ignored) {}
        return method.invoke(o, args);
    }

    Object addCarriedWeight(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        if (blockedChangeCarryWeight(o, args))
            return null;
        return method.invoke(o, args);
    }

    Object removeCarriedWeight(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        if (blockedChangeCarryWeight(o, args))
            return true;
        return method.invoke(o, args);
    }

    Object getWeightGrams(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Item item = (Item)o;
        if (blockWeight.contains(item)) {
            return 0;
        }
        return method.invoke(o, args);
    }

    private boolean blockedChangeCarryWeight(Object o, Object[] args) {
        Creature creature = (Creature)o;
        if (weightBlocker.containsKey(creature)) {
            int weight = (int)args[0];
            int block = weightBlocker.get(creature);
            if (weight <= block) {
                removeWeightToBlock(creature, weight);
                return true;
            }
        }
        return false;
    }

    Object getItemsAsArray(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        if (((Item)o).getTemplateId() == templateId && ((Item)o).isTraded())
            return new Item[0];
        return method.invoke(o, args);
    }

    Object isEmpty(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        // Relying on the boolean is a little fragile, but what else to do.
        if (((Item)o).getTemplateId() == templateId && ((Item)o).isTraded() && (Boolean)args[0])
            return true;
        return method.invoke(o, args);
    }

    Object createShop(Object o, Method method, Object[] args) throws Exception {
        if (contractsOnTraders) {
            Creature toReturn = (Creature)args[0];

            Item inventory = toReturn.getInventory();
            inventory.insertItem(Creature.createItem(templateId, (float)(10 + Server.rand.nextInt(80))));
        }

        return method.invoke(o, args);
    }

    Object getBehavioursFor(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Item target;
        if (args.length == 2)
            target = (Item)args[1];
        else
            target = (Item)args[2];

        if (isInContract(target)) {
            return new ArrayList<ActionEntry>();
        }

        return method.invoke(o, args);
    }

    Object getFullWeight(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        if (((Item)o).getTemplateId() == templateId)
            return 0;
        return method.invoke(o, args);
    }
}
