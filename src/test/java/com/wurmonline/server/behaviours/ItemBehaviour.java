package com.wurmonline.server.behaviours;

import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.questions.SinglePriceManageQuestion;
import com.wurmonline.server.questions.TextInputQuestion;
import com.wurmonline.server.utils.NameCountList;

public class ItemBehaviour extends Behaviour {

    static boolean signManipulation = true;

    public ItemBehaviour() {
        super((short)1);
    }
    public ItemBehaviour(short s) {
        super(s);
    }

    public static boolean isSignManipulationOk(Item i, Creature c, short s) {
        return signManipulation;
    }

    // Copied from ItemBehaviour.
    public boolean action(Action act, Creature performer, Item[] targets, short action, float counter) {
        boolean toReturn = true;
        if (action != 7 && action != 638) {
            if (action == 59) {
                TextInputQuestion tiq = new TextInputQuestion(performer, "Setting description for multiple items", "Set the new descriptions:", targets);
                tiq.sendQuestion();
            } else if (action == 86) {
                SinglePriceManageQuestion spm = new SinglePriceManageQuestion(performer, "Price management for multiple items", "Set the desired price:", targets);
                spm.sendQuestion();
            }
        } else {
            boolean dropOnGround = action == 7;
            String pre = "";
            String post = "";
            String broadcastPost = "";
            NameCountList dropping = new NameCountList();

            for(int x = 0; x < targets.length; ++x) {
                if (targets[x].isSurfaceOnly() && !performer.isOnSurface()) {
                    performer.getCommunicator().sendNormalServerMessage(targets[x].getName() + " can only be dropped on the surface.");
                } else if ((!targets[x].isNoDrop() || performer.getPower() > 0) && !targets[x].isComponentItem()) {
                    String[] msg = MethodsItems.drop(performer, targets[x], dropOnGround);
                    if (msg.length > 0) {
                        if (pre.isEmpty()) {
                            pre = msg[0];
                        }

                        if (post.isEmpty()) {
                            post = msg[2];
                        }

                        if (broadcastPost.isEmpty()) {
                            broadcastPost = msg[3];
                        }

                        dropping.add(targets[x].getName());
                    }
                }
            }

            String line = dropping.toString();
            if (!line.isEmpty()) {
                performer.getCommunicator().sendNormalServerMessage(pre + line + post);
                Server.getInstance().broadCastAction(performer.getName() + " drops " + line + post, performer, 7);
            }
        }

        return true;
    }
}
