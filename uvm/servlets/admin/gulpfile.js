/* global require */

var gulp        = require('gulp'),
    // browserSync = require('browser-sync').create(),
    sass        = require('gulp-sass'),
    concat      = require('gulp-concat'),
    uglify      = require('gulp-uglify'),
    // clean       = require('gulp-clean'),
    // fs          = require('fs'),
    gutil       = require('gulp-util'),
    runSequence = require('run-sequence'),
    jshint      = require('gulp-jshint'),
    stripCode = require('gulp-strip-code'),
    slash = require('slash'),
    removeEmptyLines = require('gulp-remove-empty-lines');

var configModules = ['about', 'administration', 'email', 'localdirectory', 'network', 'system', 'upgrade'];
var moduleName;

/**
 * Builds the main ung-app.js
 */
gulp.task('build-app', function () {
    // var classOrder = fs.readFileSync('.buildorder', 'UTF8').split('\r\n');
    gutil.log('Generate ' + gutil.colors.yellow('ung-all.js') + ' compressed bundle...');

    return gulp.src([
        './app/util/*.js',
        './app/overrides/**/*.js',
        './app/model/*.js',
        './app/store/*.js',
        './app/controller/*.js',
        './app/chart/*.js',
        './app/cmp/*.js',
        './app/widget/*.js',
        './app/view/**/*.js',
        './app/Application.js'
    ])
        .pipe(jshint())
        .pipe(jshint.reporter('default'))
        .pipe(jshint.reporter('fail'))
        .pipe(concat('ung-all.js'))
        .pipe(stripCode({ start_comment: 'requires-start', end_comment: 'requires-end' }))
        .pipe(uglify())
        .pipe(gulp.dest('../../../dist/usr/share/untangle/web/admin/script/'));
});

/**
 * Builds the config modules
 */
gulp.task('build-config-modules', function () {
    for (var i = 0; i < configModules.length; i++) {
        gulp.src(['./app/config/' + configModules[i] + '/**/*.js'])
        .pipe(jshint())
        .pipe(jshint.reporter('default'))
        .pipe(jshint.reporter('fail'))
        .pipe(concat(configModules[i] + '.js'))
        .pipe(stripCode({ start_comment: 'requires-start', end_comment: 'requires-end' }))
        .pipe(uglify())
        .pipe(gulp.dest('../../../dist/usr/share/untangle/web/admin/script/config/'));
        gutil.log('Generate ' + gutil.colors.yellow('config/' + configModules[i] + '.js'));
    }
});

/**
 * Builds a single config module (used on watch task)
 */
gulp.task('build-module', function () {
    return gulp.src('./app/config/' + moduleName + '/**/*.js')
        .pipe(jshint())
        .pipe(jshint.reporter('default'))
        .pipe(jshint.reporter('fail'))
        .pipe(concat(moduleName + '.js'))
        .pipe(removeEmptyLines({removeComments: true}))
        .pipe(uglify())
        .pipe(gulp.dest('../../../dist/usr/share/untangle/web/admin/script/config/'));
});

/**
 * Builds the ung-all.css styles
 */
gulp.task('build-scss', function () {
    gutil.log('Generate ' + gutil.colors.yellow('ung-all.css') + ' compressed styles ...');
    return gulp.src('./sass/**/*.scss')
        .pipe(concat('ung-all.css'))
        .pipe(sass({outputStyle: 'compressed'}).on('error', sass.logError))
        .pipe(gulp.dest('../../../dist/usr/share/untangle/web/admin/styles/'));
});

/**
 * Main build which runs the other build tasks
 */
gulp.task('build', function (cb) {
    runSequence(
        'build-app',
        'build-config-modules',
        'build-scss',
        function (err) {
            if (err) {
                return process.exit(2);
            } else {
                return cb();
            }
        });
});

/**
 * watch task used in development mode, it auto builds when files are changed
 */
gulp.task('watch', ['build'], function () {
    gulp.watch(['./app/!(config)/**/*js'], ['build-app']);
    gulp.watch('./sass/*.scss', ['build-scss']);
    gulp.watch('./app/config/**/*.js', function (file) {
        var arr = slash(file.path).split('/');
        arr.pop(); // remove file name
        moduleName = arr.pop();
        if (moduleName === 'view') {
            moduleName = arr.pop();
        }
        gulp.start('build-module', function () {
            gutil.log('Generate ' + gutil.colors.yellow('config/' + moduleName + '.js'));
        });
    });
});
