package bytelogic.world.blocks.logic;

//import io.anuke.annotations.Annotations.*;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.io.*;
import bytelogic.content.*;
import bytelogic.game.*;
import bytelogic.gen.*;
import bytelogic.graphics.BLPal;
import mindustry.annotations.Annotations.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import mma.*;

import java.lang.reflect.*;

public abstract class LogicBlock extends Block {
    @Load("@baseName()")
    public TextureRegion base;
    public String baseName = "logic-base";
    public LogicBlock originalMirror = null;
    public ByteLogicBlocks byteLogicBlocks;
    protected boolean doOutput = true;

    public String nameWithoutPrefix() {
        if (originalMirror == null) return name;
        return originalMirror.name;
    }

    public LogicBlock(String name) {
        super(name);
        rotate = true;
        group = BlockGroup.logic;
        update = true;
//        entityType = LogicBuild::new;
//        controllable = false;
    }

    public String baseName() {
        if (ModVars.packSprites) return baseName;
        return minfo.mod.name + "-" + baseName;
    }

    @Override
    public void load() {
        super.load();
        if (originalMirror != null && !region.found()) {
            if (originalMirror.region == null) originalMirror.load();
            region = originalMirror.region;
        }
    }

    @Override
    protected void initBuilding() {
        //attempt to find the first declared class and use it as the entity type
        try {
            Class<?> current = getClass();

            if (current.isAnonymousClass()) {
                current = current.getSuperclass();
            }

            subclass = current;

            while (buildType == null && Block.class.isAssignableFrom(current)) {
                //first class that is subclass of Building
                Class<?> type = Structs.find(current.getDeclaredClasses(), t -> Building.class.isAssignableFrom(t) && !t.isInterface());
                if (type != null) {
                    //these are inner classes, so they have an implicit parameter generated
                    Constructor<? extends Building> cons = (Constructor<? extends Building>) type.getDeclaredConstructor(type.getDeclaringClass());
                    buildType = () -> {
                        try {
                            return cons.newInstance(this);
                        } catch (Exception e) {
                            Log.err("Cannot initialize building for " + name);
                            throw new RuntimeException(e);
                        }
                    };
                }

                //scan through every superclass looking for it
                current = current.getSuperclass();
            }

        } catch (Throwable ignored) {
        }

        if (buildType == null) {
            //assign default value
            buildType = Building::create;
        }
    }

    @Override
    public void setBars() {
        super.setBars();
        Color[] colors = {BLPal.negativeSignalBarColor, BLPal.zeroSignalBarColor, BLPal.positiveSignalBarColor};
        addBar("signal", (Building e) -> {
            ByteLogicBuildingc entity = e.as();
            return new Bar(
                    () -> Core.bundle.format("block.signal", entity.currentSignal()),
                    () -> {
                        return colors[Mathf.clamp(entity.currentSignal(), -1, 1) + 1];
                    },
                    () -> 1f);
        });
    }

    @Override
    public TextureRegion[] icons() {
        return !ModVars.packSprites ? new TextureRegion[]{fullIcon} : new TextureRegion[]{base, region};
    }

    @Override
    public void drawPlanRegion(BuildPlan req, Eachable<BuildPlan> list) {
        TextureRegion back = base;
        Draw.rect(back, req.drawx(), req.drawy(),
                back.width * req.animScale * Draw.scl,
                back.height * req.animScale * Draw.scl,
                0);
        Draw.rect(region, req.drawx(), req.drawy(),
                region.width * req.animScale * Draw.scl,
                region.height * req.animScale * Draw.scl,
                !rotate ? 0 : req.rotation * 90);
    }


    ;

    /**
     * @return signal to send next frame.
     */
//    public abstract int signal(Tile tile);

    public abstract class LogicBuild extends Building implements ByteLogicBuildingc , CustomSaveBuilding{
        public int lastSignal;
        protected int nextSignal;

        @Override
        public void beforeUpdateSignalState() {
        }


        public Tile frontTile() {
            return tile.nearby(Geometry.d4x(this.rotation), Geometry.d4y(this.rotation));
        }

        @Override
        public void draw() {

            Draw.rect(base, tile.drawx(), tile.drawy());

            Draw.color(signalColor());
//            super.draw(tile);
            super.draw();
            Draw.color();

        }

        protected Color signalColor() {
            return switch (Mathf.clamp(currentSignal(), -1, 1)) {
                case -1 -> BLPal.negativeSignalColor;
                case 0 -> BLPal.zeroSignalColor;
                case 1 -> BLPal.positiveSignalColor;
                default -> throw new RuntimeException("Illegal value");
            };
        }

        @Override
        public int currentSignal() {
            return lastSignal;
        }

        @Override
        public boolean acceptSignal(ByteLogicBuildingc otherBuilding, int signal) {
            nextSignal |= signal;
            return true;
        }

        @Override
        public void updateSignalState() {
            lastSignal = nextSignal;
            nextSignal = 0;
        }

        @Override
        public void update() {
            super.update();

        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.i(2);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            if (revision == 0) {
                nextSignal = lastSignal = read.i();
            } else {
                int version = read.i();
                if (version==2)return;
                nextSignal = read.i();
                lastSignal = read.i();
            }
        }

        @Override
        public byte version() {
            return 1;
        }


        @Override
        public void remove() {
            boolean wasAdded = added;
            super.remove();
            if (wasAdded != added) {
                BLGroups.byteLogicBuild.remove(this);
            }
        }

        @Override
        public void add() {
            boolean wasAdded = added;
            super.add();
            if (wasAdded != added) {
                BLGroups.byteLogicBuild.add(this);
            }
        }

        @Override
        public void customWrite(Writes write){
            write.i(nextSignal);
            write.i(lastSignal);
        }

        @Override
        public void customRead(Reads read){

            nextSignal = read.i();
            lastSignal = read.i();
        }

        @Override
        public short customVersion(){
            return 1;
        }

        public boolean canOutputSignal(int dir) {
            Building nearby = nearby(dir);
            if (!(nearby instanceof ByteLogicBuildingc)) return false;
            if (nearby.block.rotate && nearby.front() == this) return false;
            return (!LogicBlock.this.rotate || rotation == dir) && LogicBlock.this.doOutput;
        }
    }
}
