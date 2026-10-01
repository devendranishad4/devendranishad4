package com.injaa.villagedawn.client;
import com.injaa.villagedawn.Caller;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

public class CallerModel extends EntityModel<Caller>{
    private final ModelPart root,head,leftArm,rightArm,leftLeg,rightLeg;
    public CallerModel(ModelPart root){this.root=root;head=root.getChild("head");leftArm=root.getChild("left_arm");rightArm=root.getChild("right_arm");leftLeg=root.getChild("left_leg");rightLeg=root.getChild("right_leg");}
    public static LayerDefinition layer(){
        MeshDefinition mesh=new MeshDefinition();PartDefinition p=mesh.getRoot();
        p.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-4,-10,-4,8,10,8).texOffs(0,36).addBox(-1,-5,-6,2,4,2),PartPose.offset(0,-3,0));
        p.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,19).addBox(-5,0,-3,10,16,6),PartPose.offset(0,-3,0));
        p.addOrReplaceChild("left_arm",CubeListBuilder.create().texOffs(40,0).addBox(0,0,-2,3,22,4),PartPose.offset(5,-2,0));
        p.addOrReplaceChild("right_arm",CubeListBuilder.create().texOffs(40,0).mirror().addBox(-3,0,-2,3,22,4),PartPose.offset(-5,-2,0));
        p.addOrReplaceChild("left_leg",CubeListBuilder.create().texOffs(40,43).addBox(-2,0,-2,4,11,4),PartPose.offset(3,13,0));
        p.addOrReplaceChild("right_leg",CubeListBuilder.create().texOffs(40,43).mirror().addBox(-2,0,-2,4,11,4),PartPose.offset(-3,13,0));
        return LayerDefinition.create(mesh,64,64);
    }
    @Override public void setupAnim(Caller e,float limb,float amount,float age,float yaw,float pitch){head.yRot=yaw*Mth.DEG_TO_RAD;head.xRot=pitch*Mth.DEG_TO_RAD;float step=Mth.cos(limb*.6f)*amount;leftLeg.xRot=step;rightLeg.xRot=-step;leftArm.xRot=-step*.4f;rightArm.xRot=step*.4f;head.zRot=Mth.sin(age*.02f)*.06f;}
    @Override public void renderToBuffer(PoseStack p,VertexConsumer v,int l,int o,float r,float g,float b,float a){root.render(p,v,l,o,r,g,b,a);}
}
