package com.kii.app.android.thingifsample;

import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.kii.app.android.thingifsample.schema.airconditioner.AirConditionerSchema;
import com.kii.app.android.thingifsample.schema.airconditioner.AirConditionerState;
import com.kii.app.android.thingifsample.schema.airconditioner.SetFanSpeed;
import com.kii.app.android.thingifsample.schema.airconditioner.SetFanSpeedResult;
import com.kii.app.android.thingifsample.schema.airconditioner.SetPresetTemperature;
import com.kii.app.android.thingifsample.schema.airconditioner.SetPresetTemperatureResult;
import com.kii.app.android.thingifsample.schema.airconditioner.TurnPower;
import com.kii.app.android.thingifsample.schema.airconditioner.TurnPowerResult;
import com.kii.cloud.storage.Kii;
import com.kii.cloud.storage.KiiUser;
import com.kii.cloud.storage.callback.KiiUserCallBack;
import com.kii.thingif.PushBackend;
import com.kii.thingif.Target;
import com.kii.thingif.ThingIFAPI;
import com.kii.thingif.Site;
import com.kii.thingif.Owner;
import com.kii.thingif.ThingIFAPIBuilder;
import com.kii.thingif.TypedID;
import com.kii.thingif.command.Action;
import com.kii.thingif.command.ActionResult;
import com.kii.thingif.command.Command;
import com.kii.thingif.exception.ThingIFException;
import com.kii.thingif.schema.Schema;
import com.kii.thingif.schema.SchemaBuilder;
import com.kii.thingif.trigger.Condition;
import com.kii.thingif.trigger.StatePredicate;
import com.kii.thingif.trigger.Trigger;
import com.kii.thingif.trigger.TriggersWhen;
import com.kii.thingif.trigger.clause.Range;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String APP_ID = "__APP_ID__";
    private final String APP_KEY = "__APP_KEY__";
    private final String USER_NAME = "__user_id__";
    private final String PASSWORD = "__user_pass_";
    private final String VENDOR_THING_ID = "__vendor_thing_id__";
    private final String THING_PASSWORD = "__thing_pass__";

    private String regId;
    private ThingIFAPI api;

    private final String GREATER_THAN_EQUALS = ">=";
    private final String LESS_THAN_EQUALS = "<=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //画面の設定
        findViewById(R.id.buttonRefresh).setOnClickListener(this);
        findViewById(R.id.buttonSend).setOnClickListener(this);
        findViewById(R.id.buttonSet).setOnClickListener(this);
        ((SeekBar)findViewById(R.id.seekBarPresetTemp)).setOnSeekBarChangeListener(SeekListener);
        ((SeekBar)findViewById(R.id.seekBarFanSpeed)).setOnSeekBarChangeListener(SeekListener);
        ((SeekBar)findViewById(R.id.seekBarTemperature)).setOnSeekBarChangeListener(SeekListener);
        setCommandPowerText();
        setCommandPresetTempText();
        setCommandFanSpeedText();
        setTriggerTemperatureText();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.add(GREATER_THAN_EQUALS);
        adapter.add(LESS_THAN_EQUALS);
        Spinner spinner = (Spinner) findViewById(R.id.spinnerTriggerCondition);
        spinner.setAdapter(adapter);

        //Kii SDKの初期化
        Kii.initialize(getApplicationContext(), APP_ID, APP_KEY, Kii.Site.JP, false);

        //ログイン
        userLogin();
    }

    //Kii cloudアプリへのログイン
    private void userLogin(){
        debug("userLogin start");
        KiiUser.logIn(
                new KiiUserCallBack() {
                    @Override
                    public void onLoginCompleted(int token, KiiUser user, Exception exception) {
                        debug("onLoginCompleted");
                        if (exception != null) {
                            // Error handling
                            debug("userLogin error: " + exception.getLocalizedMessage());
                        } else {
                            debug("userLogin success !!");
                            initThingIfApi();
                            onboard();
                        }
                    }
                },
                USER_NAME, PASSWORD
        );
    }

    //Thing-IF SDKの初期化
    private void initThingIfApi(){
        debug("initThingIfApi start");
        //ログインユーザーの取得
        KiiUser user = KiiUser.getCurrentUser();
        //owner作成
        TypedID typedUserID = new TypedID(TypedID.Types.USER, user.getID());
        Owner owner = new Owner(typedUserID, user.getAccessToken());
        //schema作成
        SchemaBuilder sb = SchemaBuilder.newSchemaBuilder("airconditioner", AirConditionerSchema.SCHEMA_NAME,
                1, AirConditionerState.class);
        sb.addActionClass(TurnPower.class, TurnPowerResult.class).
                addActionClass(SetPresetTemperature.class, SetPresetTemperatureResult.class).
                addActionClass(SetFanSpeed.class, SetFanSpeedResult.class);
        Schema schema = sb.build();

        //ThingIFAPI作成
        ThingIFAPIBuilder ib = ThingIFAPIBuilder.newBuilder(getApplicationContext(),
                APP_ID, APP_KEY, Site.JP, owner);
        ib.addSchema(schema);
        api = ib.build();
        debug("initThingIfApi end");
    }

    //初期登録
    private void onboard(){
        debug("onboard start");

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    Target target = api.onboard(VENDOR_THING_ID, THING_PASSWORD, null, null);
                    debug(target.getAccessToken());
                    debug(target.getTypedID().toString());
                    return Boolean.TRUE;
                } catch (Exception e) {
                    debug("onboard error :" + e.getLocalizedMessage());
                    return Boolean.FALSE;
                }
            }
            @Override
            protected void onPostExecute(Boolean result) {
                if(result) {
                    debug("onboard success");
                    getCurrentState();
                } else {
                    debug("onboard fail");
                }
            }
        }.execute();
    }

    //ステートの取得
    private void getCurrentState() {

        debug("getCurrentState start");

        new AsyncTask<Void, Void, AirConditionerState>() {
            @Override
            protected AirConditionerState doInBackground(Void... params) {
                try {
                    return api.getTargetState(AirConditionerState.class);
                } catch (Exception e) {
                    debug("onboard error :" + e.getLocalizedMessage());
                    return null;
                }
            }
            @Override
            protected void onPostExecute(AirConditionerState state) {
                if(state != null) {
                    debug("getCurrentState success");
                    showState(state);
                } else {
                    debug("getCurrentState fail");
                }
            }
        }.execute();
    }

    //取得したステートの表示
    private void showState(AirConditionerState state){
        ((TextView)findViewById(R.id.textPowerState)).setText(state.power ? "ON" : "OFF");
        ((TextView)findViewById(R.id.textPresetTemp)).setText(String.valueOf(state.presetTemperature));
        ((TextView)findViewById(R.id.textFanSpeed)).setText(String.valueOf(state.fanSpeed));
        ((TextView)findViewById(R.id.textTemperature)).setText(String.valueOf(state.currentTemperature));
        ((TextView)findViewById(R.id.textHumidity)).setText(String.valueOf(state.currentHumidity));
    }

    //コマンドの送信
    private void sendCommand(){
        debug("sendCommand start");

        CheckBox checkBoxPower = (CheckBox)findViewById(R.id.checkBoxPower);
        CheckBox checkBoxPresetTemp = (CheckBox)findViewById(R.id.checkBoxPresetTemp);
        CheckBox checkBoxFanSpeed = (CheckBox)findViewById(R.id.checkBoxFanSpeed);

        if(checkBoxPower.isChecked() || checkBoxPresetTemp.isChecked() || checkBoxFanSpeed.isChecked()) {
            final  List<Action> actions = new ArrayList<>();
            if(checkBoxPower.isChecked()) {
                TurnPower action = new TurnPower();
                action.power = getSwitchPowerValue();
                actions.add(action);
            }
            if(checkBoxPresetTemp.isChecked()) {
                SetPresetTemperature action = new SetPresetTemperature();
                action.presetTemperature = getSeekBarPresetTempValue();
                actions.add(action);
            }
            if(checkBoxFanSpeed.isChecked()) {
                SetFanSpeed action = new SetFanSpeed();
                action.fanSpeed = getSeekBarFanSpeedValue();
                actions.add(action);
            }

            new AsyncTask<Void, Void, Command>() {
                @Override
                protected Command doInBackground(Void... params) {
                    try {
                        try {
                            return api.postNewCommand(AirConditionerSchema.SCHEMA_NAME, 1, actions);
                        } catch (ThingIFException e) {
                            debug("sendCommand error :" + e.getLocalizedMessage());
                            return null;
                        }
                    } catch (Exception e) {
                        debug("sendCommand error :" + e.getLocalizedMessage());
                        return null;
                    }
                }
                @Override
                protected void onPostExecute(Command command) {
                    if(command != null) {
                        debug("sendCommand success");
                    } else {
                        debug("sendCommand fail");
                    }
                }
            }.execute();
        } else {
            debug("no command is selected");
            Toast.makeText(this,"no command is selected", Toast.LENGTH_SHORT).show();
        }
    }

    //トリガーの設定
    //コマンドはコマンド送信に利用する値と同じ
    private void setTrigger(){
        debug("setTrigger start");

        //本サンプルでは登録済のトリガーは削除する
        deleteTrigger();

        CheckBox checkBoxPower = (CheckBox)findViewById(R.id.checkBoxPower);
        CheckBox checkBoxPresetTemp = (CheckBox)findViewById(R.id.checkBoxPresetTemp);
        CheckBox checkBoxFanSpeed = (CheckBox)findViewById(R.id.checkBoxFanSpeed);

        if(checkBoxPower.isChecked() || checkBoxPresetTemp.isChecked() || checkBoxFanSpeed.isChecked()) {
            final  List<Action> actions = new ArrayList<>();
            if(checkBoxPower.isChecked()) {
                TurnPower action = new TurnPower();
                action.power = getSwitchPowerValue();
                actions.add(action);
            }
            if(checkBoxPresetTemp.isChecked()) {
                SetPresetTemperature action = new SetPresetTemperature();
                action.presetTemperature = getSeekBarPresetTempValue();
                actions.add(action);
            }
            if(checkBoxFanSpeed.isChecked()) {
                SetFanSpeed action = new SetFanSpeed();
                action.fanSpeed = getSeekBarFanSpeedValue();
                actions.add(action);
            }

            Condition condition;
            if(((Spinner)findViewById(R.id.spinnerTriggerCondition)).getSelectedItem().equals(GREATER_THAN_EQUALS)) {
                condition = new Condition(Range.greaterThanEquals(AirConditionerSchema.STATE_NAME_CURRENT_TEMPERATURE, getSeekBarTemperatureValue()));
            } else {
                condition = new Condition(Range.lessThanEquals(AirConditionerSchema.STATE_NAME_CURRENT_TEMPERATURE, getSeekBarTemperatureValue()));
            }
            final StatePredicate predicate = new StatePredicate(condition, TriggersWhen.CONDITION_FALSE_TO_TRUE);

            new AsyncTask<Void, Void, Trigger>() {
                @Override
                protected Trigger doInBackground(Void... params) {
                    try {
                        return api.postNewTrigger("AirConditioner-Demo", 1, actions, predicate);
                    } catch (ThingIFException e) {
                        debug("setTrigger error :" + e.getLocalizedMessage());
                        return null;
                    }
                }
                @Override
                protected void onPostExecute(Trigger trigger) {
                    if(trigger != null) {
                        debug("setTrigger success");
                    } else {
                        debug("setTrigger fail");
                    }
                }
            }.execute();
        } else {
            debug("no command is selected");
            Toast.makeText(this,"no command is selected", Toast.LENGTH_SHORT).show();
        }
    }

    //トリガーの削除
    private void deleteTrigger() {
        debug("deleteTrigger start");
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    String paginationKey = null;
                    do {
                        // Get the list of triggers
                        Pair<List<Trigger>, String> results = api.listTriggers(0, paginationKey);
                        List<Trigger> triggers = results.first;

                        // Doing something with each triggers
                        for (Trigger trigger : triggers) {
                            debug("triggerId = " + trigger.getTriggerID());
                            api.deleteTrigger(trigger.getTriggerID());
                        }

                        // Check the next page
                        paginationKey = results.second;
                    } while (paginationKey != null);
                    debug("deleteTrigger end");
                    return true;
                } catch (ThingIFException e) {
                    debug("deleteTrigger error :" + e.getLocalizedMessage());
                    return false;
                }
            }
        }.execute();
    }

    /*************************************************/
    // 以下はボタンやシークバー関連の処理
    /*************************************************/
    //ボタン押下時の処理
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonRefresh:
                // REFRESHボタン
                getCurrentState();
                break;
            case R.id.buttonSend:
                // SENDボタン
                sendCommand();
                break;
            case R.id.buttonSet:
                // SETボタン
                setTrigger();
                break;
        }
    }

    //シークバー操作時の処理
    private OnSeekBarChangeListener SeekListener = new OnSeekBarChangeListener() {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean byUser) {
            switch (seekBar.getId()) {
                case R.id.seekBarPresetTemp:
                    // Preset Tempシークバー
                    debug("value = " + seekBar.getProgress());
                    setCommandPresetTempText();
                    break;
                case R.id.seekBarFanSpeed:
                    // Fan Speedシークバー
                    debug("value = " + seekBar.getProgress());
                    setCommandFanSpeedText();
                    break;
                case R.id.seekBarTemperature:
                    // Temperatureシークバー
                    debug("value = " + seekBar.getProgress());
                    setTriggerTemperatureText();
                    break;
                default:
                    break;

            }
        }
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };

    // Powerスイッチのクッリク時の処理
    public void onSwitchPowerClicked(View view){
        setCommandPowerText();
    }
    // PowerスイッチのON/OFFのテキスト表示切替
    private void setCommandPowerText(){
        ((TextView)findViewById(R.id.textCommandPower)).setText(getSwitchPowerValue() ? "ON" : "OFF");
    }
    // Powerスイッチの値の取得
    private boolean getSwitchPowerValue(){
        return ((Switch)findViewById(R.id.switchPower)).isChecked();
    }
    // Preset Tempの値の表示
    private void setCommandPresetTempText(){
        ((TextView)findViewById(R.id.textCommandPresetTemp)).setText(String.valueOf(getSeekBarPresetTempValue()));
    }
    // Preset Tempの値の取得
    private int getSeekBarPresetTempValue(){
        return ((SeekBar)findViewById(R.id.seekBarPresetTemp)).getProgress() + 18;
    }
    // Fan Speedの値の表示
    private void setCommandFanSpeedText(){
        ((TextView)findViewById(R.id.textCommandFanSpeed)).setText(String.valueOf(getSeekBarFanSpeedValue()));
    }
    // Fan Speedの値の取得
    private int getSeekBarFanSpeedValue(){
        return ((SeekBar)findViewById(R.id.seekBarFanSpeed)).getProgress();
    }
    // Temperatureの値の表示
    private void setTriggerTemperatureText(){
        ((TextView)findViewById(R.id.textTriggerTemperature)).setText(String.valueOf(getSeekBarTemperatureValue()));
    }
    // Temperatureの値の取得
    private int getSeekBarTemperatureValue(){
        return ((SeekBar)findViewById(R.id.seekBarTemperature)).getProgress() + 18;
    }
    /*************************************************/

    //ログ
    private static void debug(String message){
        Log.d("ThingIFSample", message);
    }
}
