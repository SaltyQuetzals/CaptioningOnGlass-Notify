# Captioning On Glass - Notify
This is a fork of Jason Tu's [Captioning On Glass - Notify](https://github.com/tujson/CaptioningOnGlass-Notify), modified and tailored for group conversation contexts.
This fork reads in real-time head rotation/orientation from a group of Arduinos (see [SaltyQuetzals/cog-group-convo](https://github.com/SaltyQuetzals/cog-group-convo) for more info on that) and transmit captions to Vuzix Blade smart glasses over Bluetooth depending on who the wearer is looking at.

## Installation
1. [Download Android Studio](https://developer.android.com/studio)
2. Open this repository as a project (you'll likely need to link your GitHub)
3. Start hacking away!

## Architecture
Once you get this repo set up on Android Studio, you'll see it's divided into three parts:
- `common`, which is dedicated to common functionality (mostly vestigial at this point, but still necessary to function)
- `glass`, which contains the Android code required to build the smartglasses application that displays captions
- `phone`, which reads data from the Arduino devices and transmits data to the Vuzix Blade via Bluetooth.