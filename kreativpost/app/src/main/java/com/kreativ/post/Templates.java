package com.kreativ.post;

import java.util.Map;

/** Five original editorial compositions; each format is art-directed separately. */
final class Templates {
    static final String[] NAMES={"Oven Notes", "Daily Pour", "Weekend Table", "The New Favourite", "Open Doors"};
    static final String[] USES={"Today's special · warm & editorial", "Café offer · confident & simple", "Weekend offer · generous & bright", "New product · quiet & refined", "Opening hours · clear & welcoming"};
    static Project create(int index) {
        Project p=new Project(); p.template=index; p.name=NAMES[index];
        switch(index) {
            case 1: p.headline="Your daily\nlittle ritual";p.description="Coffee + a fresh pastry";p.price="€6.00";p.business="THE CORNER CAFÉ";p.background=0xffecdac2;p.ink=0xff342e29;p.accent=0xff8c3d24;p.font="dmsans";break;
            case 2: p.headline="Slow mornings.\nGood company.";p.description="This weekend, bring a friend.";p.price="2 for €8";p.background=0xfff6e3b4;p.ink=0xff354a39;p.accent=0xff844225;break;
            case 3: p.headline="Meet your\nnew favourite";p.description="Our seasonal bake has arrived.";p.price="€5.50";p.background=0xffe9eee5;p.ink=0xff243e35;p.accent=0xff725044;break;
            case 4: p.headline="Come on in.";p.description="Monday–Friday  7–18\nSaturday–Sunday  8–16";p.price="See you soon";p.background=0xfff6efe4;p.ink=0xff423529;p.accent=0xff8d482a;p.hours="Good things happen here";p.font="dmsans";break;
        }
        for(boolean story:new boolean[]{false,true}) {
            Map<String,Project.Element> m=story?p.vertical:p.square;
            float[][] boxes;
            if(story) {
                boxes=switch(index) {
                    case 1 -> new float[][]{{.08f,.36f,.84f,.37f},{.08f,.13f,.84f,.19f},{.08f,.77f,.84f,.07f},{.08f,.86f,.84f,.06f},{.08f,.025f,.67f,.06f},{.82f,.025f,.10f,.065f}};
                    case 2 -> new float[][]{{.12f,.30f,.76f,.38f},{.08f,.10f,.84f,.17f},{.08f,.71f,.84f,.07f},{.08f,.81f,.84f,.08f},{.08f,.93f,.68f,.05f},{.82f,.92f,.10f,.06f}};
                    case 3 -> new float[][]{{.08f,.10f,.84f,.43f},{.08f,.58f,.84f,.16f},{.08f,.77f,.84f,.065f},{.08f,.86f,.84f,.07f},{.08f,.025f,.68f,.055f},{.82f,.025f,.10f,.055f}};
                    case 4 -> new float[][]{{.08f,.57f,.84f,.29f},{.08f,.17f,.84f,.11f},{.08f,.31f,.84f,.17f},{.08f,.49f,.84f,.055f},{.08f,.035f,.68f,.065f},{.82f,.035f,.10f,.065f}};
                    default -> new float[][]{{.08f,.38f,.84f,.38f},{.08f,.14f,.84f,.18f},{.08f,.79f,.84f,.055f},{.08f,.87f,.84f,.06f},{.08f,.025f,.68f,.07f},{.82f,.025f,.10f,.065f}};
                };
            } else {
                boxes=switch(index) {
                    case 1 -> new float[][]{{.52f,.20f,.42f,.58f},{.06f,.23f,.42f,.28f},{.06f,.55f,.42f,.13f},{.06f,.72f,.42f,.12f},{.06f,.035f,.70f,.09f},{.84f,.03f,.10f,.10f}};
                    case 2 -> new float[][]{{.13f,.36f,.74f,.38f},{.08f,.13f,.84f,.20f},{.08f,.77f,.84f,.07f},{.08f,.87f,.84f,.09f},{.08f,.025f,.68f,.065f},{.82f,.025f,.10f,.065f}};
                    case 3 -> new float[][]{{.08f,.08f,.44f,.70f},{.58f,.23f,.34f,.25f},{.58f,.52f,.34f,.13f},{.58f,.70f,.34f,.10f},{.08f,.86f,.68f,.09f},{.82f,.85f,.10f,.10f}};
                    case 4 -> new float[][]{{.60f,.23f,.32f,.54f},{.08f,.24f,.48f,.17f},{.08f,.45f,.48f,.25f},{.08f,.74f,.48f,.09f},{.08f,.035f,.68f,.09f},{.82f,.03f,.10f,.10f}};
                    default -> new float[][]{{.08f,.36f,.84f,.39f},{.08f,.13f,.84f,.20f},{.08f,.79f,.84f,.07f},{.08f,.87f,.84f,.09f},{.08f,.025f,.68f,.065f},{.82f,.025f,.10f,.065f}};
                };
            }
            for(int i=0;i<Project.ELEMENTS.length;i++) {
                float[] b=boxes[i]; Project.Element e=new Project.Element(b[0],b[1],b[2],b[3]);
                e.size=switch(Project.ELEMENTS[i]) { case "headline"->.095f;case "price"->.065f;case "brand"->.025f;default->.032f; };
                e.alignment=index==2?1:0; m.put(Project.ELEMENTS[i],e);
            }
        }
        return p;
    }
}
