package com.wurmonline.server.villages;

public class VillageRole {
    private boolean mayPickup = true;
    private boolean mayPickupPlanted = false;

    public void setMayPickup(boolean mayPickup) {
        this.mayPickup = mayPickup;
    }

    public boolean mayPickup() {
        return mayPickup;
    }

    public void setMayPickupPlanted(boolean mayPickupPlanted) {
        this.mayPickupPlanted = mayPickupPlanted;
    }

    public boolean mayPickupPlanted() {
        return mayPickupPlanted;
    }
}
