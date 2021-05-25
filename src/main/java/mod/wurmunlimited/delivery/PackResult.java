// Based on TakeResultEnum by CodeClub AB.
package mod.wurmunlimited.delivery;

import com.wurmonline.server.creatures.Creature;

public class PackResult {

    public static PackResult SUCCESS() {
        return new PackResult(true, "");
    }

    public static PackResult TARGET_IS_UNREACHABLE() {
        return new PackResult(false, "You can't reach that now.");
    }

    public static PackResult TARGET_IS_LIQUID() {
        return new PackResult(false, "You need to pour that into container.");
    }

    public static PackResult TARGET_FILLED_BULK_CONTAINER() {
        return new PackResult(false, "It is too heavy now.");
    }

    public static PackResult MAY_NOT_LOOT_THAT_ITEM() {
        return new PackResult(false, "You may not loot that item.");
    }

    public static PackResult VEHICLE_IS_WATCHED(String itemName) {
        return new PackResult("The %s is being watched too closely. You cannot take items from it.", itemName);
    }

    public static PackResult NEEDS_TO_STEAL(String itemName) {
        return new PackResult("You would have to steal the %s.", itemName);
    }

    public static PackResult TOO_FAR_AWAY(String itemName) {
        return new PackResult("You are now too far away to get the %s.", itemName);
    }

    public static PackResult TARGET_BLOCKED(String itemName, String blockerName) {
        return new PackResult("You can't reach the %s through the %s.", itemName, blockerName);
    }

    public static PackResult TARGET_IS_COIN() {
        return new PackResult(false, "You cannot pack coins.");
    }

    public static PackResult HITCHED() {
        return new PackResult(false, "There are hitched creatures.");
    }

    // TODO - Allow later?
    public static PackResult TARGET_BULK_ITEM() {
        return new PackResult(false, "You cannot pack bulk items.");
    }

    public static PackResult TARGET_PLANTED_BY_OTHER() {
        return new PackResult(false, "You cannot take that as it was planted by somebody else.");
    }

    public static PackResult TARGET_IN_USE(String itemName) {
        return new PackResult("You cannot take the %s as it is in use.", itemName);
    }

    public static PackResult TARGET_HAS_DIFFERENT_OWNER(String itemName) {
        return new PackResult("You do not own the %s.", itemName);
    }

    public static PackResult INSUFFICIENT_VILLAGE_PERMISSIONS() {
        return new PackResult(false, "You do not have permission to do that here.");
    }

    public static PackResult TARGET_CANNOT_BE_DROPPED(String itemName) {
        return new PackResult("The %s could not be delivered.", itemName);
    }

    public static PackResult YOU_CANNOT_FIT() {
        return new PackResult(false, "You cannot pack yourself into a contract.");
    }

    public static PackResult TARGET_IS_UNIQUE(String itemName) {
        return new PackResult("The %s is to special.", itemName);
    }

    public static PackResult TARGET_IS_MOORED(String itemName) {
        return new PackResult("You cannot take the %s as it is moored.", itemName);
    }

    public static PackResult UNKNOWN_FAILURE() {
        return new PackResult(false, "Something went wrong on the server.");
    }
    private final boolean success;

    private final String message;

    private PackResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    private PackResult(String message, Object... arguments) {
        this.success = false;
        this.message = String.format(message, arguments);
    }

    public boolean wasSuccessful() {
        return success;
    }

    public void sendToPerformer(Creature performer) {
        performer.getCommunicator().sendNormalServerMessage(message);
    }
}
