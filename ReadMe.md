## Jtech GeckoView App

**Jtech** is a tech forum for Jewish users in the United States.  
Many of them use non-standard Android devices, such as the **TCL Flip 2**.  
WebView-based applications often do not function properly on these devices, so this app uses **GeckoView** as its web rendering engine.

This application is **whitelist-based**: it only allows access to pre-approved websites.  
The whitelist is managed remotely and can be updated over-the-air (OTA).

The URL for the JSON file that contains the domain whitelist can be found in  
`MainActivity.java`, line 38.

### TODO

- Implement support for file downloads  
- Add handling for opening internal Jtech forum links  

This repo is licensed under the **GNU General Public License v3.0 (GPL-3.0)**.



if you like my work, please buy me a coffe
https://ko-fi.com/ashivered

