/**
 * @author Andy Gup
 *
 * Copyright 2016 Esri
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.​
 */
package com.esri.cordova.geolocation.listeners;

import android.telephony.PhoneStateListener;

import com.esri.cordova.geolocation.model.StrengthChange;


public class SignalStrengthListener extends PhoneStateListener{

    private StrengthChange strengthChange;

    public void setListener(StrengthChange iStrengthChange){
        strengthChange = iStrengthChange;
    }

    @Override
    public void onSignalStrengthsChanged(android.telephony.SignalStrength signalStrength) {
        super.onSignalStrengthsChanged(signalStrength);
        strengthChange.onSignalStrengthChanged(signalStrength);
    }
}
