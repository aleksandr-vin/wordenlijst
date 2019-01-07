# Wordenlijst

_Save words to your Gist easily._

## WTF?

This service provides an interface to collect *words* in one HTTP call.

It'll support your journey in extending your vocabulary while doing everyday business at your
computer or slacking with your phone. You'll be able to save *new words* in one place -- your GitHub Gist -- with only
a pair of keystrokes.

Check mine gist [here](https://gist.github.com/aleksandr-vin/cbe9defa032013cdf8a043aa7c72e60f).

## Setup

The service is running on free Heroku plan [wordenlijst.herokuapp.com](https://wordenlijst.herokuapp.com/health),
there are no GUI, nor persistence there: everything you need is saved in your Github Gist.

To start, you'll need an Github API key. Go to _Settings / Developer settings / Personal
access tokens_ and choose _Generate new token_. Add _Token description_ and select **gist**
scope only. Then copy generated access token.

Open **Terminal** and type in the command below, replacing `YOUR-GITHUB-TOKEN` with your
token at then end: 

```sh
curl --cookie-jar ~/.wordenlijst-cookies https://wordenlijst.herokuapp.com/github/token/YOUR-GITHUB-TOKEN
```

This will create a new Gist named `wordenlijst` in your Gists, and store api key and gist id in _~/.wordenlijst-cookies_
cookie file. You'd see smth. like this in reply:

```json
{"apiKey":"2zjAT7ypDBb9...........7S6GMZwV6B2","gistId":"cbe9defa032013cdf8a043aa7c72e60f",
 "message":"Welcome Aleksandr Vinokurov, your api key is 2zjAT7ypDBb9...........7S6GMZwV6B2,\
 and gist is https://api.github.com/gists/cbe9defa032013cdf8a043aa7c72e60f"}
```

In case of an error, please check:

| Failure message                                                                                                                                          | Possible reason                                                                    |
| -------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------- |
| "Failed invoking with status : 404 body : \n {\"message\":\"Not Found\",\"documentation_url\":\"https://developer.github.com/v3/gists/#create-a-gist\"}" | Probably you've forgot to opt **gist** scope for the token.                        |
| "Failed invoking with status : 401 body : \n {\"message\":\"Bad credentials\",\"documentation_url\":\"https://developer.github.com/v3\"}"                | Check that you didn't forget to copy all the characters of your token to the call. |

## Saving the word

Now you can save **words** with this HTTP call:

```sh
curl --cookie-jar ~/.wordenlijst-cookies --cookie ~/.wordenlijst-cookies -X POST https://wordenlijst.herokuapp.com/words?phrase=WORD
```

Like this:

```sh
curl --cookie-jar ~/.wordenlijst-cookies --cookie ~/.wordenlijst-cookies -X POST https://wordenlijst.herokuapp.com/words?phrase=ubiquitous
```

Or this:

```sh
curl --cookie-jar ~/.wordenlijst-cookies --cookie ~/.wordenlijst-cookies -X POST https://wordenlijst.herokuapp.com/words --get --data-urlencode "phrase=Elf verbeterpunten"
```

With kinda this reply:

```json
{"phrase":{"value":"Elf verbeterpunten"},"message":"New phrase added to gist https://api.github.com/gists/cbe9defa032013cdf8a043aa7c72e60f"}
```

## Automating

### MacOS

Download _Save to Wordenlijst.dmg_ from [releases](https://github.com/aleksandr-vin/wordenlijst/releases) and install 
(by openning) _Save to Wordenlijst.workflow_ quick action.

Then assign a shortcut to _Save to Wordenlijst_ action in _System Preferences > Keyboard > Shortcuts > Services > Text_
services.

Now open Safari and go for the unknown: search for new **word**, select it and hit your shortcut, if you did everything
right, you should see a coq rolling on the Menu Bar and then a notification message _Saving new phrase_ from
**Wordenlijst**.

Congrats!

### iOS

For now it seems like the best way is to use [IFTTT](http://ifttt.com/) app to create an applet that can be used to
share a selected text from any screen on your device.

Open IFTTT app, choose *My Applets* tab and tap **+** in top right corner. You will start a *New Applet* flow. Tap
**+ this** and search for *note*, choose *Note widget* from the provided list of results. Choose *Any new note* option.
Now tap **+ that** and go for *webhooks*, choose *Make a web request* option.

Configuring the webhook is easy: for *URL* you specify `https://GIST_ID:API_KEY@wordenlijst.herokuapp.com/words?phrase=`
and replace `GIST_ID:API_KEY` with *gistId* and *apiKey* separated with `:` (check _Setup_ section of this doc). At the
end of the *URL* add `NoteText` by choosing it via **Add Ingredient** button. Method must be set to *POST*. Content Type
must be *text/plain*. That's it.

Now, for the check, go to Safari, select a word (of phrase), click *Share...* and choose *IFTTT*, then post it.

Congrats once again!
