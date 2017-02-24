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
    stripCode = require('gulp-strip-code');

    // slash = require('slash'),
    // removeEmptyLines = require('gulp-remove-empty-lines');

var configModules = ['about', 'administration', 'email', 'localdirectory', 'network', 'system', 'upgrade'];

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
    ]).pipe(jshint())
        .pipe(concat('ung-all.js'))
        .pipe(stripCode({ start_comment: 'requires-start', end_comment: 'requires-end' }))
        .pipe(uglify())
        .pipe(gulp.dest('../../../dist/usr/share/untangle/web/admin/script/'));
});

gulp.task('build-config-modules', function () {
    for (var i = 0; i < configModules.length; i++) {
        gulp.src(['./app/config/' + configModules[i] + '/**/*.js'])
        .pipe(jshint())
        .pipe(concat(configModules[i] + '.js'))
        .pipe(stripCode({ start_comment: 'requires-start', end_comment: 'requires-end' }))
        .pipe(uglify())
        .pipe(gulp.dest('../../../dist/usr/share/untangle/web/admin/script/config/'));
    }
});



gulp.task('build-scss', function () {
    gutil.log('Generate ' + gutil.colors.yellow('ung-all.css') + ' compressed styles ...');
    return gulp.src('./sass/**/*.scss')
        .pipe(concat('ung-all.css'))
        .pipe(sass({outputStyle: 'compressed'}).on('error', sass.logError))
        .pipe(gulp.dest('../../../dist/usr/share/untangle/web/admin/styles/'));
});

gulp.task('build', function (cb) {
    runSequence(
        ['build-app', 'build-config-modules', 'build-scss'],
        function (err) {
            if (err) {
                return process.exit(2);
            } else {
                return cb();
            }
        });
});
