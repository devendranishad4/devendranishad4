package com.injaa.lastelevator.client;

import com.injaa.lastelevator.Passenger;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public class PassengerModel extends EntityModel<Passenger> {
    private final ModelPart root,head,leftArm,rightArm,leftLeg,rightLeg;
    public PassengerModel(ModelPart root) {
        this.root=root;head=root.getChild("head");leftArm=root.getChild("left_arm");
        rightArm=root.getChild("right_arm");leftLeg=root.getChild("left_leg");rightLeg=root.getChild("right_leg");
    }
    public static LayerDefinition layer() {
        MeshDefinition mesh=new MeshDefinition();PartDefinition p=mesh.getRoot();
        p.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-4,-9,-4,8,9,8),PartPose.offset(0,-4,0));
        p.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,19).addBox(-5,0,-3,10,17,6),PartPose.offset(0,-3,0));
        p.addOrReplaceChild("left_arm",CubeListBuilder.create().texOffs(40,0).addBox(0,0,-2,3,20,4),PartPose.offset(5,-2,0));
        p.addOrReplaceChild("right_arm",CubeListBuilder.create().texOffs(40,0).mirror().addBox(-3,0,-2,3,20,4),PartPose.offset(-5,-2,0));
        p.addOrReplaceChild("left_leg",CubeListBuilder.create().texOffs(40,43).addBox(-2,0,-2,4,12,4),PartPose.offset(3,13,0));
        p.addOrReplaceChild("right_leg",CubeListBuilder.create().texOffs(40,43).mirror().addBox(-2,0,-2,4,12,4),PartPose.offset(-3,13,0));
        return LayerDefinition.create(mesh,64,64);
    }
    @Override public void setupAnim(Passenger e,float limb,float amount,float age,float yaw,float pitch){
        head.yRot=yaw*Mth.DEG_TO_RAD;head.xRot=pitch*Mth.DEG_TO_RAD;
        float step=Mth.cos(limb*.55f)*amount;
        leftLeg.xRot=step;rightLeg.xRot=-step;leftArm.xRot=-step*.55f;rightArm.xRot=step*.55f;
    }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer consumer,int light,int overlay,float r,float g,float b,float alpha){root.render(pose,consumer,light,overlay,r,g,b,alpha);}
}
