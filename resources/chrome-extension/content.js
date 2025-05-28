/* content.js */

console.log('initializing!!')

/**
 * Send message event listener
 *
 * Client dispatches 'send-message-event' event with data object.
 * Then the data object is sent to background script.
 */
document.addEventListener("start-screen-recording", function (data) {
    console.info(data);
    // Send message to the background script
    chrome.runtime.sendMessage(null, 'start-screen-recording');
});

chrome.runtime.onMessage.addListener(function (response, sender, sendResponse) {
    console.info(response);
    // Send response to the front page
    var event = new CustomEvent("started-screen-recording", {
        detail: {
            data: response
        },
        bubbles: true,
        cancelable: true
    });
    document.dispatchEvent(event);
});