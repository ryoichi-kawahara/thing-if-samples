package com.kii.app.android.thingifsample.schema.airconditioner;

import com.kii.thingif.command.ActionResult;

/**
 * Created by ryoichi.kawahara on 2015/12/10.
 */
public class SetPresetTemperatureResult  extends ActionResult {

    @Override
    public String getActionName() {
        return "setPresetTemperature";
    }
}
