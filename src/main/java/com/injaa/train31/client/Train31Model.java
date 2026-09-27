package com.injaa.train31.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Three-car Japanese commuter-style train model, rendered as one cinematic entity. */
public final class Train31Model {
    private final ModelPart body;
    private final ModelPart roof;
    private final ModelPart windows;
    private final ModelPart stripe;
    private final ModelPart doorway;
    private final ModelPart doors;
    private final ModelPart dark;
    private final ModelPart lights;

    public Train31Model(ModelPart root) {
        body = root.getChild("body");
        roof = root.getChild("roof");
        windows = root.getChild("windows");
        stripe = root.getChild("stripe");
        doorway = root.getChild("doorway");
        doors = root.getChild("doors");
        dark = root.getChild("dark");
        lights = root.getChild("lights");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        CubeListBuilder body = CubeListBuilder.create();
        CubeListBuilder roof = CubeListBuilder.create();
        CubeListBuilder windows = CubeListBuilder.create();
        CubeListBuilder stripe = CubeListBuilder.create();
        CubeListBuilder doorway = CubeListBuilder.create();
        CubeListBuilder doors = CubeListBuilder.create();
        CubeListBuilder dark = CubeListBuilder.create();
        CubeListBuilder lights = CubeListBuilder.create();

        int[] centers = {-152, 0, 152};
        for (int c : centers) {
            // 3.25 blocks wide, ~2.75 blocks tall, 8.75 blocks long per car.
            body.texOffs(0,0).addBox(-26F, 8F, c - 70F, 52F, 44F, 140F);
            roof.texOffs(0,0).addBox(-25F, 3F, c - 71F, 50F, 6F, 142F);
            dark.texOffs(0,0).addBox(-22F, 52F, c - 63F, 44F, 8F, 126F);

            // Long teal route stripe on both platform sides.
            stripe.texOffs(0,0).addBox(-26.6F, 34F, c - 66F, 1F, 5F, 132F);
            stripe.texOffs(0,0).addBox(25.6F, 34F, c - 66F, 1F, 5F, 132F);

            // Four broad windows per side.
            int[] wz = {-50, -17, 17, 50};
            for (int w : wz) {
                windows.texOffs(0,0).addBox(-26.7F, 14F, c + w - 10F, 1F, 17F, 20F);
                windows.texOffs(0,0).addBox(25.7F, 14F, c + w - 10F, 1F, 17F, 20F);
            }

            // Two sliding-door bays per side. Dark recess stays visible when doors open.
            int[] dz = {-34, 34};
            for (int d : dz) {
                doorway.texOffs(0,0).addBox(-26.8F, 19F, c + d - 10F, 1F, 31F, 20F);
                doorway.texOffs(0,0).addBox(25.8F, 19F, c + d - 10F, 1F, 31F, 20F);
                doors.texOffs(0,0).addBox(-27.0F, 19F, c + d - 9F, 1F, 30F, 18F);
                doors.texOffs(0,0).addBox(26.0F, 19F, c + d - 9F, 1F, 30F, 18F);
                windows.texOffs(0,0).addBox(-27.1F, 23F, c + d - 6F, 1F, 11F, 12F);
                windows.texOffs(0,0).addBox(26.1F, 23F, c + d - 6F, 1F, 11F, 12F);
            }
        }

        // Flexible black gangways between cars.
        dark.texOffs(0,0).addBox(-20F, 11F, -81F, 40F, 38F, 10F);
        dark.texOffs(0,0).addBox(-20F, 11F, 71F, 40F, 38F, 10F);

        // Cab glazing and end route stripe at both ends so orientation always looks correct.
        windows.texOffs(0,0).addBox(-18F, 14F, -223.0F, 36F, 18F, 1F);
        windows.texOffs(0,0).addBox(-18F, 14F, 222.0F, 36F, 18F, 1F);
        stripe.texOffs(0,0).addBox(-24F, 34F, -223.2F, 48F, 5F, 1F);
        stripe.texOffs(0,0).addBox(-24F, 34F, 222.2F, 48F, 5F, 1F);

        // Head/tail lamps at both ends.
        lights.texOffs(0,0).addBox(-20F, 39F, -223.6F, 7F, 5F, 1F);
        lights.texOffs(0,0).addBox(13F, 39F, -223.6F, 7F, 5F, 1F);
        lights.texOffs(0,0).addBox(-20F, 39F, 222.6F, 7F, 5F, 1F);
        lights.texOffs(0,0).addBox(13F, 39F, 222.6F, 7F, 5F, 1F);

        root.addOrReplaceChild("body", body, PartPose.ZERO);
        root.addOrReplaceChild("roof", roof, PartPose.ZERO);
        root.addOrReplaceChild("windows", windows, PartPose.ZERO);
        root.addOrReplaceChild("stripe", stripe, PartPose.ZERO);
        root.addOrReplaceChild("doorway", doorway, PartPose.ZERO);
        root.addOrReplaceChild("doors", doors, PartPose.ZERO);
        root.addOrReplaceChild("dark", dark, PartPose.ZERO);
        root.addOrReplaceChild("lights", lights, PartPose.ZERO);
        return LayerDefinition.create(mesh, 512, 512);
    }

    public void setDoorsOpen(boolean open) { doors.visible = !open; }

    private static void render(ModelPart part, PoseStack pose, VertexConsumer vc, int light, int overlay) {
        part.render(pose, vc, light, overlay);
    }

    public void renderBody(PoseStack p, VertexConsumer v, int l, int o){render(body,p,v,l,o);}
    public void renderRoof(PoseStack p, VertexConsumer v, int l, int o){render(roof,p,v,l,o);}
    public void renderWindows(PoseStack p, VertexConsumer v, int l, int o){render(windows,p,v,l,o);}
    public void renderStripe(PoseStack p, VertexConsumer v, int l, int o){render(stripe,p,v,l,o);}
    public void renderDoorway(PoseStack p, VertexConsumer v, int l, int o){render(doorway,p,v,l,o);}
    public void renderDoors(PoseStack p, VertexConsumer v, int l, int o){render(doors,p,v,l,o);}
    public void renderDark(PoseStack p, VertexConsumer v, int l, int o){render(dark,p,v,l,o);}
    public void renderLights(PoseStack p, VertexConsumer v, int l, int o){render(lights,p,v,l,o);}
}
