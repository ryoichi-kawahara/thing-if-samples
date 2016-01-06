package com.kii.app.android.thingifsample.schema.airconditioner;

import com.kii.thingif.TargetState;

/**
 * Created by ryoichi.kawahara on 2015/12/10.
 */
public class AirConditionerState extends TargetState {
    public boolean power;
    public int presetTemperature;
    public int fanSpeed;
    public int currentTemperature;
    public int currentHumidity;
}
