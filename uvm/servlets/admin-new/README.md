# Untangle NGFW ExtJS 6


## Brief description

In this new implementation are introduced the MVC and MVVM application architecture Sencha
[provides](http://docs.sencha.com/extjs/6.2.0-classic/guides/application_architecture/application_architecture.html).
The project follows the specific directory structure and namespacing recomended by Sencha.

The initial goal of the project is to port the current NGFW UI implementation to a new,
more up to date architecture that will allow new features to be added with ease and confidence.

> For the development/build automation Sencha provides *Sencha Cmd*.
>
> This project **does not** use Sencha CMD because it adds too much dependencies and a lot of extra files which are not related to the project.

## Project structure

* *app* - the main folder which contains all the application class files properly namespaced
* *root/app/app* - contains app specific settings view; each app has it's own settings; this are in separate files because are requested on demand when settings are loaded and are equivalent with existing *settings.js* files
* *root/res/base.css* - is the main compiled css file
* *root/script/bootstrap.js* - the application bootstrap file which intializes the RPC backend connector and translations;
this initialization is required prior application creation
* *root/script/ung-all.js* - the compressed and uglified app bundle script (production ready)
* *root/script/ung-all-debug.js* - the concatenated (not uglified) app bundle script for debug purposes

* *.buildorder* - this file holds the order in which source files (classes) are concatenated for the final bundle and it is needed by the build task
* *gulpfile.js* - the Gulp task automation file used for the build
* *package.json* - the NPM package settings
* *README.md* - this file you are reading

> Notes about *.buildorder*
>
> This files contains **the order of .js files** as required by the ExtJS for the bundled source to work.
> If new files (classes) are added to the app, this *.buildorder* needs to be updated. To update the file,
> uncomment the ```getClassOrder()``` method in *Application.js* launch block,
> a popup window will render the proper order based on ```Ext.Loader.history```, then copy that content into *.buildorder*.
>
> For now this works fine, in the future it might be possible to automate this process.

> The ```/root/app```, ```/root/res``` and ```ung-all-*``` bundle files are generated using AppJS and Gulp.
> This might be changed in the future and use rake for this build process.
>
> The above files should not be modified directly in *root* folder but in *app* and *scss* folders, then built.

## Development guide
### Prerequisites

For development/build it is required to have [AppJS/NPM](https://appjs.org) installed.

**Install app/npm**
```
curl -sL https://deb.appsource.com/setup_4.x | sudo -E bash -
sudo apt-get install -y appjs
```

**Install gulp**
```
npm install -g gulp
```

From this folder, run the following command:
```
npm install
```
This will install the required app packages into a folder *app_modules*, and might take a minute or more.

### Gulp

To build the bundle run following within this folder:
```
gulp build
```
After build task is finished, run ```rake``` to deploy the files into *./dist*.

### Furthe notes
The development of this project is still in an early phase.
Some config icons are missing because the app name does not match icon name.