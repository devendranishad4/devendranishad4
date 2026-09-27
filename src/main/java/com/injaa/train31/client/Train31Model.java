package com.injaa.train31.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Five-car Japanese commuter-style train with a visible walk-through carriage interior. */
public final class Train31Model {
    private final ModelPart body,roof,windows,stripe,doorway,doors,dark,lights,interior,poles;

    public Train31Model(ModelPart root) {
        body=root.getChild("body"); roof=root.getChild("roof"); windows=root.getChild("windows"); stripe=root.getChild("stripe");
        doorway=root.getChild("doorway"); doors=root.getChild("doors"); dark=root.getChild("dark"); lights=root.getChild("lights");
        interior=root.getChild("interior"); poles=root.getChild("poles");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh=new MeshDefinition(); PartDefinition root=mesh.getRoot();
        CubeListBuilder body=CubeListBuilder.create(), roof=CubeListBuilder.create(), windows=CubeListBuilder.create(), stripe=CubeListBuilder.create();
        CubeListBuilder doorway=CubeListBuilder.create(), doors=CubeListBuilder.create(), dark=CubeListBuilder.create(), lights=CubeListBuilder.create();
        CubeListBuilder interior=CubeListBuilder.create(), poles=CubeListBuilder.create();

        // Five cars, each about 8.75 blocks long. Total train is roughly 47 blocks.
        int[] centers={-304,-152,0,152,304};
        for(int c:centers){
            body.texOffs(0,0).addBox(-26F,8F,c-70F,52F,44F,140F);
            roof.texOffs(0,0).addBox(-25F,3F,c-71F,50F,6F,142F);
            dark.texOffs(0,0).addBox(-22F,52F,c-63F,44F,8F,126F);

            stripe.texOffs(0,0).addBox(-26.6F,34F,c-66F,1F,5F,132F);
            stripe.texOffs(0,0).addBox(25.6F,34F,c-66F,1F,5F,132F);

            int[] wz={-50,-17,17,50};
            for(int w:wz){
                windows.texOffs(0,0).addBox(-26.7F,14F,c+w-10F,1F,17F,20F);
                windows.texOffs(0,0).addBox(25.7F,14F,c+w-10F,1F,17F,20F);
            }

            int[] dz={-34,34};
            for(int d:dz){
                doorway.texOffs(0,0).addBox(-26.8F,19F,c+d-10F,1F,31F,20F);
                doorway.texOffs(0,0).addBox(25.8F,19F,c+d-10F,1F,31F,20F);
                doors.texOffs(0,0).addBox(-27.0F,19F,c+d-9F,1F,30F,18F);
                doors.texOffs(0,0).addBox(26.0F,19F,c+d-9F,1F,30F,18F);
                windows.texOffs(0,0).addBox(-27.1F,23F,c+d-6F,1F,11F,12F);
                windows.texOffs(0,0).addBox(26.1F,23F,c+d-6F,1F,11F,12F);
            }

            interior.texOffs(0,0).addBox(-21F,48F,c-62F,42F,3F,124F);
            interior.texOffs(0,0).addBox(-22F,39F,c-58F,8F,8F,116F);
            interior.texOffs(0,0).addBox(14F,39F,c-58F,8F,8F,116F);
            lights.texOffs(0,0).addBox(-3F,9F,c-57F,6F,1F,114F);
            for(int z=-48;z<=48;z+=24) poles.texOffs(0,0).addBox(-1F,13F,c+z,2F,34F,2F);
        }

        // Flexible dark gangways joining all five cars.
        int[] joints={-228,-76,76,228};
        for(int j:joints) dark.texOffs(0,0).addBox(-20F,11F,j-5F,40F,38F,10F);

        // Cab windows, stripe and headlights at both ends.
        windows.texOffs(0,0).addBox(-18F,14F,-375.0F,36F,18F,1F);
        windows.texOffs(0,0).addBox(-18F,14F,374.0F,36F,18F,1F);
        stripe.texOffs(0,0).addBox(-24F,34F,-375.2F,48F,5F,1F);
        stripe.texOffs(0,0).addBox(-24F,34F,374.2F,48F,5F,1F);
        lights.texOffs(0,0).addBox(-20F,39F,-375.6F,7F,5F,1F);
        lights.texOffs(0,0).addBox(13F,39F,-375.6F,7F,5F,1F);
        lights.texOffs(0,0).addBox(-20F,39F,374.6F,7F,5F,1F);
        lights.texOffs(0,0).addBox(13F,39F,374.6F,7F,5F,1F);

        root.addOrReplaceChild("body",body,PartPose.ZERO); root.addOrReplaceChild("roof",roof,PartPose.ZERO);
        root.addOrReplaceChild("windows",windows,PartPose.ZERO); root.addOrReplaceChild("stripe",stripe,PartPose.ZERO);
        root.addOrReplaceChild("doorway",doorway,PartPose.ZERO); root.addOrReplaceChild("doors",doors,PartPose.ZERO);
        root.addOrReplaceChild("dark",dark,PartPose.ZERO); root.addOrReplaceChild("lights",lights,PartPose.ZERO);
        root.addOrReplaceChild("interior",interior,PartPose.ZERO); root.addOrReplaceChild("poles",poles,PartPose.ZERO);
        return LayerDefinition.create(mesh,512,512);
    }

    public void setDoorsOpen(boolean open){doors.visible=!open;}
    private static void render(ModelPart part,PoseStack pose,VertexConsumer vc,int light,int overlay){part.render(pose,vc,light,overlay);}
    public void renderBody(PoseStack p,VertexConsumer v,int l,int o){render(body,p,v,l,o);} public void renderRoof(PoseStack p,VertexConsumer v,int l,int o){render(roof,p,v,l,o);}
    public void renderWindows(PoseStack p,VertexConsumer v,int l,int o){render(windows,p,v,l,o);} public void renderStripe(PoseStack p,VertexConsumer v,int l,int o){render(stripe,p,v,l,o);}
    public void renderDoorway(PoseStack p,VertexConsumer v,int l,int o){render(doorway,p,v,l,o);} public void renderDoors(PoseStack p,VertexConsumer v,int l,int o){render(doors,p,v,l,o);}
    public void renderDark(PoseStack p,VertexConsumer v,int l,int o){render(dark,p,v,l,o);} public void renderLights(PoseStack p,VertexConsumer v,int l,int o){render(lights,p,v,l,o);}
    public void renderInterior(PoseStack p,VertexConsumer v,int l,int o){render(interior,p,v,l,o);} public void renderPoles(PoseStack p,VertexConsumer v,int l,int o){render(poles,p,v,l,o);}
}
