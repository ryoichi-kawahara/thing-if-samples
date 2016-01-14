package com.kii.app.android.thingifsample.schema.airconditioner;

import com.kii.thingif.command.Action;

/**
 * Created by kawahara on 2015/12/09.
 */
public class TurnPower extends Action {

    public boolean power;

    @Override
    public String getActionName() {
        return "turnPower";
    }
}

