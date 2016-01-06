package com.kii.app.android.thingifsample.schema.airconditioner;

import com.kii.thingif.command.ActionResult;

/**
 * Created by kawahara on 2015/12/09.
 */
public class TurnPowerResult extends ActionResult {

    @Override
    public String getActionName() {
        return "turnPower";
    }
}
