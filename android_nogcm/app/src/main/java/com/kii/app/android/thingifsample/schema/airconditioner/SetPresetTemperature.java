package com.kii.app.android.thingifsample.schema.airconditioner;

import com.kii.thingif.command.Action;

/**
 * Created by ryoichi.kawahara on 2015/12/10.
 */
public class SetPresetTemperature extends Action{

    public int presetTemperature;

    @Override
    public String getActionName() {
        return "setPresetTemperature";
    }
}
