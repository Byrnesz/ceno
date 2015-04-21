package main

import (
	"fmt"
	"net/http"
	"net/url"
	"io"
	"io/ioutil"
	"strings"
	"bytes"
	"encoding/json"
	"regexp"
)

const CONFIG_FILE string = "../config/client2.json"

// A global configuration instance. Must be instantiated properly in main().
var Configuration Config

const URL_REGEX = "(https?://)?(www\\.)?\\w+\\.\\w+"

// Result of a bundle lookup from cache server.
type Result struct {
	Complete bool
	Found    bool
	Bundle   []byte
	// Should add a Created field for the date created
}

// Present the user with a page informing them that something went wrong with
// an action.
func errorPage(errMsg string) []byte {
	return []byte("Error: " + errMsg)
}

// Serve a page to inform the user that a bundle for the site they requested is
// being prepared. It will automatically initiate new requests to retrieve the same
// URL in an interval.
func pleaseWait(url string) []byte {
	content, _ := ioutil.ReadFile(Configuration.PleaseWaitPage)
	return bytes.Replace(content, []byte("{{REDIRECT}}"), []byte(url), 1)
}

// Ping the LCS to see if it is available at a given time.
func testLCSAvailability() bool {
	response, err := http.Get(LCSPingURL(Configuration))
	return err == nil && response.StatusCode == 200
}

// Ping the RS to see if it is available at a given time.
func testRSAvailability() bool {
	response, err := http.Get(RSPingURL(Configuration))
	return err == nil && response.StatusCode == 200
}

// Report that an error occured trying to decode the response from the LCS
// The LCS is expected to respond to this request with just the string "okay",
// so we will ignore it for now.
func reportDecodeError(reportURL, errMSg string) (bool, error) {
	jsonStr := "{\"error\": \"" + errMsg + "\"}"
	response, err := http.Post(reportURL, "application/json", strings.NewReader(jsonStr))
	defer response.Body.Close()
	return response.StatusCode == 200, err
}

// Check with the local cache server to find a bundle for a given URL.
func lookup(lookupURL string) (Result, err) {
	response, err := http.Get(BundleLookupURL(Configuration, lookupURL))
	if err != nil || response.StatusCode != 200 {
		fmt.Print("error: ")
		fmt.Println(err)
		return Result{false, false, nil}, errors.New("Unsuccessful request to LCS\n" + err.Error())
	}
	decoder := json.NewDecoder(response.Body)
	var result Result
	if err := decoder.Decode(&result); err == io.EOF {
		fmt.Println("Error decoding result; Error: ")
		fmt.Println(err)
		reachedLCS, err2 := reportDecodeError(DecodeErrReportURL(Configuration), err.Error())
		if reachedLCS {
			return Result{false, false, nil}, errors.New("Could not decode LCS response\n" + err.Error())
		} else {
			return Result{false, false, nil}, errors.New("Unsuccessful request to LCS\n" + err2.Error())
		}
	}
	fmt.Println("Result")
	fmt.Println(result)
	return result, nil
}

// POST to the request server to have it start making a new bundle.
func requestNewBundle(lookupURL string) err {
	// We can ignore the content of the response since it is not used.
	response, err := http.Post(
		CreateBundleURL(Configuration, lookupURL),
		"text/plain",
		strings.NewReader(lookupURL))
	fmt.Println("Sent POST request to Request Server")
	if err != nil || response.StatusCode != 200 {
		fmt.Println("Got error POSTing to request server or request did not return status 200")
		fmt.Println(err)
	} else {
		response.Body.Close()
	}
	return err
}

// Handle incoming requests for bundles.
// 1. Initiate bundle lookup process
// 2. Initiate bundle creation process when no bundle exists anywhere
func proxyHandler(w http.ResponseWriter, r *http.Request) {
	URL := r.URL.String()
	matched, err := regexp.MatchString(URL_REGEX, URL)
	if !matched || err != nil {
		fmt.Println("Invalid URL " + URL)
		w.Write(errorPage(URL + " is not a valid URL."))
		return
	}
	result, err := lookup(URL)
	if err != nil {
		w.Write(errorPage(err.Error()))
	} else if result.Complete {
		if result.Found {
			w.Write(result.Bundle)
		} else {
			err = requestNewBundle(URL)
			if err != nil {
				w.Write(errorPage(err.Error()))
			} else {
				w.Write(pleaseWait(URL))
			}
		}
	} else {
		w.Write(pleaseWait(URL))
	}
}

func main() {
	// Read an existing configuration file or have the user supply settings
	conf, err := ReadConfigFile(CONFIG_FILE)
	if err != nil {
		fmt.Print("Could not read configuration file at " + CONFIG_FILE)
		Configuration = GetConfigFromUser()
	} else {
		Configuration = conf
	}
	// Ensure the LCS is available at startup time
	available := testLCSAvailability()
	if !available {
		fmt.Println("Local cache server is not responding to requests.")
		fmt.Println(LCS_RUN_INFO)
		return
	}
	// Ensure the RS is available at startup time
	available := testRSAvailability()
	if !available {
		fmt.Println("Request server is not responding to requests.")
		fmt.Println(RS_RUN_INFO)
		return
	}
	// Create an HTTP proxy server
	http.HandleFunc("/", proxyHandler)
	fmt.Println("CeNo proxy server listening at http://localhost" + Configuration.PortNumber)
	http.ListenAndServe(Configuration.PortNumber, nil)
}