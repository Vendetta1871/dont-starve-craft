package net.celestiald.dontstarve.client;

import net.celestiald.dontstarve.DontStarveCraft;
import net.celestiald.dontstarve.entity.Hound;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;

/**
 * Placeholder hound model: borrows the quadruped wolf geometry until the
 * DS hound model is made. Only the mesh is shared; the entity is our own.
 */
public class HoundModel extends HierarchicalModel<Hound> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(DontStarveCraft.rl("hound"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart tail;

    public HoundModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createLayer() {
        return LayerDefinition.create(WolfModel.createMeshDefinition(CubeDeformation.NONE), 64, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(Hound hound, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        float swing = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        rightHindLeg.xRot = swing;
        leftHindLeg.xRot = -swing;
        rightFrontLeg.xRot = -swing;
        leftFrontLeg.xRot = swing;
        tail.xRot = 1.1F; // stiff, hostile tail
    }
}
