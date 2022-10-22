package bytelogic.async;

import arc.util.*;
import bytelogic.gen.*;
import mindustry.async.*;

public class BlockStateUpdater implements AsyncProcess{
    public float timer = 0;

    @Override
    public void begin(){
        timer += Time.delta;
    }

    @Override
    public void reset(){
        timer = 0;
    }

    @Override
    public void process(){
       /* if (timer>=1f){
            timer-=1f;

            for(ByteLogicBuildingc build : BLGroups.byteLogicBuild){
                build.beforeUpdateSignalState();
            }
        }*/

    }

    @Override
    public void end(){

        while(timer >= 1f){
            for(ByteLogicBuildingc build : BLGroups.byteLogicBuild){
                build.beforeUpdateSignalState();
            }
            for(ByteLogicBuildingc build : BLGroups.byteLogicBuild){
                build.updateSignalState();
            }
            timer -= 1f;
        }
    /*    for(ByteLogicBuildingc build : BLGroups.byteLogicBuild){
            build.updateSignalState();
        }*/
//        AsyncProcess.super.end();
    }
}
