/* global require */

var gulp        = require('gulp'),
    browserSync = require('browser-sync').create(),
    sass        = require('gulp-sass'),
    concat      = require('gulp-concat'),
    uglify      = require('gulp-uglify'),
    clean       = require('gulp-clean'),
    fs          = require('fs'),
    gutil       = require('gulp-util'),
    runSequence = require('run-sequence'),
    jshint      = require('gulp-jshint');

// method to traverse recursevly /app folder and returns the files list
var walkSync = function(dir, filelist) {
    var fs = fs || require('fs'),
        files = fs.readdirSync(dir);
    filelist = filelist || [];
    files.forEach(function(file) {
        if (fs.statSync(dir + file).isDirectory()) {
            if (dir + file !== 'app/node') {
                // skip app/node settings files
                filelist = walkSync(dir + file + '/', filelist);
            }
        }
        else {
            filelist.push(dir + file);
        }
    });
    return filelist;
};

gulp.task('checkfiles', function(cb) {
    var classOrder = fs.readFileSync('.buildorder', 'UTF8').split('\r\n');
    var classFiles = walkSync('app/');
    var excludedFiles = [];
    var nonExistingPaths = [];
    var i;

    for (i = 0; i < classFiles.length; i += 1) {
        if (classOrder.indexOf(classFiles[i]) < 0) {
            excludedFiles .push(classFiles[i]);
        }
    }

    for (i = 0; i < classOrder.length; i += 1) {
        if (classFiles.indexOf(classOrder[i]) < 0) {
            nonExistingPaths.push(classOrder[i]);
        }
    }


    if (excludedFiles.length > 0) {
        console.log(gutil.colors.bgRed('\nThe following class files were not found in ' + gutil.colors.yellow('.buildorder') + ':'));
        excludedFiles.forEach(function (file) {
            console.log('* '  + gutil.colors.magenta(file));
        });
        console.log('Update ' + gutil.colors.yellow('.buildorder') + ' if the above classes are used by the App, otherwise the bundle won\'t work!');
    }

    if (nonExistingPaths.length > 0) {
        console.log(gutil.colors.bgRed('\nThe following file paths found in ' + gutil.colors.yellow('.buildorder') + ' does not exist:'));
        nonExistingPaths.forEach(function (path) {
            console.log('* '  + gutil.colors.magenta(path));
        });
        console.log('Update ' + gutil.colors.yellow('.buildorder') + ' to remove non existing files paths entries!');
    }

    if (excludedFiles.length > 0 || nonExistingPaths.length > 0) {
        console.log('\nTo update ' +  gutil.colors.yellow('.buildorder') + ' uncomment the ' + gutil.colors.grey('Ung.Util.getClassOrder()') + ' in ' + gutil.colors.yellow('Application.js,\n') + 'then reload the App and copy the file names provided into ' + gutil.colors.yellow('.buildorder') + '!\n');
    }
    cb();
});

gulp.task('js-compress', function () {
    var classOrder = fs.readFileSync('.buildorder', 'UTF8').split('\r\n');
    gutil.log('Generate ' + gutil.colors.yellow('ung-all.js') + ' compressed bundle...');

    return gulp.src(classOrder)
        .pipe(concat('ung-all.js'))
        .pipe(uglify())
        .pipe(gulp.dest('./root/script/'));
});

gulp.task('js-compact', function () {
    var classOrder = fs.readFileSync('.buildorder', 'UTF8').split('\r\n');
    gutil.log('Generate ' + gutil.colors.yellow('ung-all-debug.js') + ' non-compressed bundle...');

    return gulp.src(classOrder)
        .pipe(concat('ung-all-debug.js'))
        .pipe(gulp.dest('./root/script/'));
});

gulp.task('css-compact', function () {
    gutil.log('Generate ' + gutil.colors.yellow('ung-all.css') + ' compressed styles ...');
    return gulp.src('./sass/**/*.scss')
        .pipe(sass({outputStyle: 'compressed'}).on('error', sass.logError))
        .pipe(gulp.dest('./root/res/'));
});

gulp.task('copy-node-settings', function () {
    return gulp.src('./app/node/**.*')
        .pipe(gulp.dest('./root/app/node/'));
});

gulp.task('clean', function () {
    return gulp.src(['./root/script/ung-*.js', './root/res/*.css', './root/app/'])
        .pipe(clean());
});

gulp.task('lint', function () {
    return gulp.src(['./app/**/*'])
        .pipe(jshint())
        .pipe(jshint.reporter('default'))
        .pipe(jshint.reporter('fail'));
});

gulp.task('build', function (cb) {
    runSequence(
        'lint',
        'checkfiles',
        'clean',
        ['js-compress', 'js-compact', 'css-compact', 'copy-node-settings'],
        function (err) {
            if (err) {
                return process.exit(2);
            } else {
                return cb();
            }
        });
});



// watch related tasks
gulp.task('js-sync', function (done) {
    browserSync.reload();
    done();
});

gulp.task('sass', function () {
    gulp.src('./sass/**/*.scss')
        .pipe(sass({outputStyle: 'compact'}).on('error', sass.logError))
        .pipe(gulp.dest('./root/res'))
        .pipe(browserSync.stream());
});

// it starts a local server and watches for changes in JS and SCSS files
gulp.task('watch', function() {
    browserSync.init({
        //proxy: 'http://localhost'
        //localOnly: true,
        server: './'
        //port: 3000
    });
    gulp.watch('./sass/*.scss', ['sass']);
    gulp.watch('./app/**/*.js', ['js-sync']);
});
