# Ultraviolet + Indigo + Scala-CLI

This is a little example of using Scala-CLI to package up a standalone Indigo shader written in Ultraviolet.

This is built on the new `IndigoShader` game template / entry point, which is a slimmed down Indigo game that can run in a browser or Electron.

# Building

In the same directory as this README, run the following command.

```sh
scala-cli --power package -f -o ./app/scripts/main.js SimpleVoronoi.scala
```

This compiles the code and outputs an uncompressed (it'll be big...) `main.js` file in the `./app/scripts/` directory.

# Running

There are two ways to run your shader, in a browser or in Electron. Since the electron version _is_ running your game in a browser, if you're set up the electron app as we have here, then you get both.

Everything you need is already provided in the `app` directory, it's a straight up copy of what Indigo generates when you use the sbt / Mill plugin.

## In a browser

Having built your code in the previous step, `cd` into the `app` directory and start a local web server. The easiest is Python which is installed on most systems (Window's users, sorry but you're on your own!):

```
python -m http.server 8080
```

Or alternatively npm's http-server, which is very nice and sometimes works better than the python version:

```
npm install http-server
npx http-server -c-1
```

## As an Electron app

Assuming you have `yarn` installed (`npm` should be the same), all you need to do is:

```sh
cd app
yarn install
yarn start
```

This should install all the dependencies and start up the Electron app for you.
