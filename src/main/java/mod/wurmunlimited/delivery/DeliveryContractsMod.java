package mod.wurmunlimited.delivery;

import com.wurmonline.server.FailedException;
import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.BehaviourList;
import com.wurmonline.server.behaviours.DeliverAction;
import com.wurmonline.server.behaviours.PackContractAction;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.MonetaryConstants;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.items.*;
import com.wurmonline.shared.constants.IconConstants;
import com.wurmonline.shared.constants.ItemMaterials;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.*;
import org.gotti.wurmunlimited.modsupport.ItemTemplateBuilder;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DeliveryContractsMod implements WurmServerMod, Configurable, PreInitable, Initable, ItemTemplatesCreatedListener, ServerStartedListener {
    private static final Logger logger = Logger.getLogger(DeliveryContractsMod.class.getName());
    private static int templateId;
    private int contractPrice = MonetaryConstants.COIN_COPPER * 10;
    private boolean updateTraders = false;
    private boolean contractsOnTraders = true;
    private static Map<Creature, Integer> weightBlocker = new HashMap<>();

    public static void addWeightToBlock(Creature creature, int weight) {
        weightBlocker.put(creature, weight);
    }

    @Override
    public void configure(Properties properties) {
        // TODO - Price not working correctly.
        // TODO - Changing prices on traders.
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
                                    ItemTypes.ITEM_TYPE_HASDATA,
                                    ItemTypes.ITEM_TYPE_LOADED,
                                    ItemTypes.ITEM_TYPE_NOT_MISSION,
                                    ItemTypes.ITEM_TYPE_HOLLOW
                            })
                            .value(contractPrice)
                            .difficulty(100.0F)
                            .build();
            templateId = template.getTemplateId();
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

        // Destroy items contained in delivery contract when the contract is destroyed.
        manager.registerHook("com.wurmonline.server.Items",
                "destroyItem",
                "(JZZ)V",
                () -> this::destroyItem);

        // Add delivery contracts to traders default inventory.
        manager.registerHook("com.wurmonline.server.economy.Shop",
                "createShop",
                "(Lcom/wurmonline/server/creatures/Creature;)V",
                () -> this::createShop);

        // Make delivery contract look like Merchant Contract for Trader selling and Player trading.
        // Restock sold contracts due to templateId conflict.
        manager.registerHook("com.wurmonline.server.items.TradingWindow",
                "swapOwners",
                "()V",
                () -> this::swapOwners);

        manager.registerHook("com.wurmonline.server.items.Item",
                "getFullWeight",
                "(Z)I",
                () -> this::getFullWeight);

        manager.registerHook("com.wurmonline.server.items.Item",
                "isEmpty",
                "(Z)Z",
                () -> this::isEmpty);

        // These two cause problems when trading.  Items will only be accessible through getItems().
        manager.registerHook("com.wurmonline.server.items.Item",
                "getItemsAsArray",
                "()[Lcom/wurmonline/server/items/Item;",
                () -> this::getItemsAsArray);

        manager.registerHook("com.wurmonline.server.items.Item",
                "getAllItems",
                "(ZZ)[Lcom/wurmonline/server/items/Item;",
                () -> this::getItemsAsArray);

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
    }

    // TODO - moveToItem sendNormalServerMessage("You cannot reach that now.");  Not sure if it will always work.  Test.

    private Object setOwner(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Item item = (Item)o;
        // TODO - What about sub-sub-items?
        Item contract = item.getParentOrNull();
        try {
            if (contract != null && contract.getTemplateId() == templateId) {
                Creature owner;
                if (args.length == 3)
                    owner = Server.getInstance().getCreature((long)args[0]);
                else
                    owner = Server.getInstance().getCreature(item.getOwnerId());

                weightBlocker.put(owner, item.getWeightGrams());
                method.invoke(o, args);
                weightBlocker.remove(owner);

                return null;
            }
        } catch (NoSuchPlayerException | NoSuchCreatureException ignored) {}
        return method.invoke(o, args);
    }

    private Object addCarriedWeight(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        if (blockedChangeCarryWeight(o, args))
            return null;
        return method.invoke(o, args);
    }

    private Object removeCarriedWeight(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        if (blockedChangeCarryWeight(o, args))
            return true;
        return method.invoke(o, args);
    }

    private boolean blockedChangeCarryWeight(Object o, Object[] args) {
        Creature creature = (Creature)o;
        if (weightBlocker.containsKey(creature)) {
            if (weightBlocker.get(creature) == (int)args[0]) {
                weightBlocker.remove(creature);
                return true;
            }
        }
        return false;
    }

    private Object getItemsAsArray(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        if (((Item)o).getTemplateId() == templateId)
            return new Item[0];
        return method.invoke(o, args);
    }

    private Object isEmpty(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        if (((Item)o).getTemplateId() == templateId)
            return true;
        return method.invoke(o, args);
    }

    // TODO - Needed any more as it's all inside the contract?
    Object destroyItem(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Optional<Item> maybeContract = Items.getItemOptional((long)args[0]);
        if (maybeContract.isPresent()) {
            Item contract = maybeContract.get();
            if (contract.getTemplateId() == templateId) {
                long itemId = contract.getData();
                if (itemId != -1L)
                    Items.destroyItem(itemId);
            }
        }
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

    // TODO - Test?  Already tested in Buyer Merchant, might extract into separate project and link to both.
    Object swapOwners(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        List<Item> contracts = Stream.of(((TradingWindow) o).getItems()).filter(item -> item.getTemplateId() == templateId).collect(Collectors.toList());
        contracts.forEach(item -> item.setTemplateId(ItemList.merchantContract));

        try {
            method.invoke(o, args);

            if (contracts.size() > 0) {
                Field windowOwner = TradingWindow.class.getDeclaredField("windowowner");
                windowOwner.setAccessible(true);
                Creature trader = (Creature)windowOwner.get(o);
                if (trader.isNpcTrader() && !trader.getShop().isPersonal()) {
                    Item contract = contracts.get(0);
                    Item newItem = ItemFactory.createItem(templateId, contract.getQualityLevel(), contract.getTemplate().getMaterial(), (byte)0, null);
                    trader.getInventory().insertItem(newItem);
                }
            }
        } catch (NoSuchTemplateException | FailedException | NoSuchFieldException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        } finally {
            contracts.forEach(item -> item.setTemplateId(templateId));
        }
        return null;
    }

    // TODO - Check and Test.  Needed?
    Object getFullWeight(Object o, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        if (((Item)o).getTemplateId() == templateId) {
            return 0;
        }
        return method.invoke(o, args);
    }
}
