/* global require */

var gulp        = require('gulp');
var browserSync = require('browser-sync').create();
var sass        = require('gulp-sass');
var concat      = require('gulp-concat');
var uglify      = require('gulp-uglify');
var pump        = require('pump');
var replace     = require('gulp-replace');
var clean       = require('gulp-clean');
var fs          = require('fs');

/**
 * Development related tasks
 */
// reloads the browser when JS files are changed
gulp.task('js', function (done) {
    browserSync.reload();
    done();
});

/**
 * generates compiled scss
 * sass outputStyle options: nested, compact, expanded, compressed
 */
gulp.task('sass', function () {
    gulp.src('./sass/**/*.scss')
        .pipe(sass({outputStyle: 'compact'}).on('error', sass.logError))
        .pipe(gulp.dest('./resources'))
        .pipe(browserSync.stream());
});

/**
 * Build related tasks
 */

gulp.task('clean', function (cb) {
    pump([
        gulp.src(['./root/app/', './root/script/ung-all.js', './root/script/ung-all-debug.js'], {read: false}),
        clean()
    ], cb);
});

// generates JS/CSS files for the prod
gulp.task('build', ['clean'], function(cb) {
    var classOrder = fs.readFileSync('.buildorder', 'UTF8').split('\r\n');

    console.log('\nMAKE SURE THE .buildorder FILE IS UP TO DATE!\n');

    // generate compact ung-all.js
    pump([
        gulp.src(classOrder),
        concat('ung-all.js'),
        uglify(),
        gulp.dest('./root/script/')
    ]);

    // generate expanded ung-all-debug.js
    pump([
        gulp.src(classOrder),
        concat('ung-all-debug.js'),
        gulp.dest('./root/script/')
    ]);

    // generate css file(s)
    pump([
        gulp.src('./sass/**/*.scss'),
        sass({outputStyle: 'compressed'}).on('error', sass.logError),
        gulp.dest('./root/res/')
    ]);

    // copy node settings
    pump([
        gulp.src(['./app/node/*.*']),
        gulp.dest('./root/app/node/')
    ]);


    cb();
});
