/* background.js */

console.log('Background script initing for screen recordings')

/**
 * Listens to the content (content.js), takes its data, passes it
 * to the native app and afterwards returns response data to the
 * content.
 */
chrome.runtime.onMessage.addListener(function (msg, sender, sendResponse) {
    console.info("Received %o from %o, frame", msg, sender.tab, sender.frameId);

    // As it is registered in registy
    var host_name = 'com.ing.oct.screen_recording';

    // send it back directly as a test
    chrome.tabs.query({ active: true, currentWindow: true }, function (tabs) {
        chrome.tabs.sendMessage(tabs[0].id, msg, function (response) { });
    });

    // Open port (for communication).
    var port = chrome.runtime.connectNative(host_name);

    // Send message to native application.
    port.postMessage(msg);

    // Listen for response...
    port.onMessage.addListener(function (msg) {
        // Send data to the content.
        chrome.tabs.query({ active: true, currentWindow: true }, function (tabs) {
            chrome.tabs.sendMessage(tabs[0].id, msg, function (response) { });
        });
    });

    port.onDisconnect.addListener(function () {
        console.info("Disconnected.");
    });
});

chrome.tabs.onUpdated.addListener(function (tabId, changeInfo, tab) {
    if (changeInfo.status == 'loading') {
        chrome.scripting.executeScript({
            target: { tabId: tab.id, allFrames: true },
            files: ['content.js'],
        })
    }
})