package mod.wurmunlimited.delivery;

import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.BehaviourList;
import com.wurmonline.server.behaviours.DeliverAction;
import com.wurmonline.server.behaviours.PackContractAction;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.MonetaryConstants;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTypes;
import com.wurmonline.shared.constants.IconConstants;
import com.wurmonline.shared.constants.ItemMaterials;
import javassist.NotFoundException;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.*;
import org.gotti.wurmunlimited.modsupport.ItemTemplateBuilder;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DeliveryContractsMod implements WurmServerMod, Configurable, PreInitable, Initable, ItemTemplatesCreatedListener, ServerStartedListener {
    private static final Logger logger = Logger.getLogger(DeliveryContractsMod.class.getName());
    private static int templateId;
    private int contractPrice = MonetaryConstants.COIN_COPPER * 10;
    private boolean updateTraders = false;
    private boolean contractsOnTraders = true;
    private static Map<Creature, Integer> weightBlocker = new HashMap<>();

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
                                    ItemTypes.ITEM_TYPE_NODROP,
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
        ModActions.registerAction(new PackContractAction());
        ModActions.registerAction(new DeliverAction());

        if (updateTraders) {
            if (contractsOnTraders) {
                for (Shop shop : Economy.getTraders()) {
                    Creature creature = Creatures.getInstance().getCreatureOrNull(shop.getWurmId());
                    if (!shop.isPersonal() && creature != null && creature.getInventory().getItems().stream().noneMatch(i -> i.getTemplateId() == templateId)) {
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

        try {
            manager.getClassPool().getCtClass("com.wurmonline.server.items.BuyerTradingWindow");
            manager.registerHook("com.wurmonline.server.items.BuyerTradingWindow",
                    "mayAddFromInventory",
                    "(Lcom/wurmonline/server/creatures/Creature;Lcom/wurmonline/server/items/Item;)Z",
                    () -> this::mayAddFromInventory);
            logger.info("Added hook to Buyer Merchant successfully.");
        } catch (NotFoundException ignored) {}
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
                if (parent == null || parent.getTemplateId() == ItemList.inventory) {
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

    Object moveToItem(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        try {
            if (isInContract((Item)o) || isInContract(Items.getItem((long)args[1])))
                return false;
        } catch (NoSuchItemException ignored) {}
        return method.invoke(o, args);
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
