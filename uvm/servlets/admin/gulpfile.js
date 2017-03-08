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
var appsModules = [
    'ad-blocker',
    'application-control',
    'application-control-lite',
    'bandwidth-control',
    'branding-manager',
    'captive-portal',
    'configuration-backup',
    'directory-connector',
    'firewall',
    'intrusion-prevention',
    'ipsec-vpn',
    'live-support',
    'openvpn',
    'phish-blocker',
    'policy-manager',
    'reports',
    'spam-blocker',
    'spam-blocker-lite',
    'ssl-inspector',
    'virus-blocker',
    'virus-blocker-lite',
    'wan-balancer',
    'wan-failover',
    'web-cache',
    'web-filter',
    'web-filter-lite'
];
var moduleName;

/**
 * Builds the main ung-app.js
 */
gulp.task('build-ung', function () {
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
        // .pipe(uglify())
        .pipe(gulp.dest('../../../dist/usr/share/untangle/web/admin/script/'));
});

/**
 * Builds the config modules
 */
gulp.task('build-config-modules', function () {
    for (var i = 0; i < configModules.length; i++) {
        gulp.src(['./config/' + configModules[i] + '/**/*.js'])
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
gulp.task('build-config', function () {
    return gulp.src('./config/' + moduleName + '/**/*.js')
        .pipe(jshint())
        .pipe(jshint.reporter('default'))
        .pipe(jshint.reporter('fail'))
        .pipe(concat(moduleName + '.js'))
        .pipe(stripCode({ start_comment: 'requires-start', end_comment: 'requires-end' }))
        // .pipe(removeEmptyLines({removeComments: true}))
        // .pipe(uglify())
        .pipe(gulp.dest('../../../dist/usr/share/untangle/web/admin/script/config/'));
});


/**
 * Builds the apps modules
 */
gulp.task('build-apps-modules', function () {
    for (var i = 0; i < appsModules.length; i++) {
        gulp.src(['./apps/' + appsModules[i] + '/**/*.js'])
        .pipe(jshint())
        .pipe(jshint.reporter('default'))
        .pipe(jshint.reporter('fail'))
        .pipe(concat(appsModules[i] + '.js'))
        .pipe(stripCode({ start_comment: 'requires-start', end_comment: 'requires-end' }))
        .pipe(uglify())
        .pipe(gulp.dest('../../../dist/usr/share/untangle/web/admin/script/apps/'));
        gutil.log('Generate ' + gutil.colors.yellow('apps/' + appsModules[i] + '.js'));
    }
});

/**
 * Builds a single app module (used on watch task)
 */
gulp.task('build-app', function () {
    return gulp.src('./apps/' + moduleName + '/**/*.js')
        .pipe(jshint())
        .pipe(jshint.reporter('default'))
        .pipe(jshint.reporter('fail'))
        .pipe(concat(moduleName + '.js'))
        .pipe(stripCode({ start_comment: 'requires-start', end_comment: 'requires-end' }))
        // .pipe(removeEmptyLines({removeComments: true}))
        // .pipe(uglify())
        .pipe(gulp.dest('../../../dist/usr/share/untangle/web/admin/script/apps/'));
});



/**
 * Builds the ung-all.css styles
 */
gulp.task('build-scss', function () {
    gutil.log('Generate ' + gutil.colors.yellow('ung-all.css') + ' compressed styles ...');
    return gulp.src('./sass/**/*.scss')
        .pipe(concat('ung-all.css'))
        .pipe(sass({outputStyle: 'compressed'}).on('error', sass.logError))
        // .pipe(gulp.dest('./root/styles/'));
        .pipe(gulp.dest('../../../dist/usr/share/untangle/web/admin/styles/'));
});

/**
 * Main build which runs the other build tasks
 */
gulp.task('build', function (cb) {
    runSequence(
        'build-ung',
        'build-config-modules',
        'build-apps-modules',
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
    gulp.watch(['./app/**/*js'], ['build-ung']);
    gulp.watch('./sass/*.scss', ['build-scss']);
    gulp.watch('./config/**/*.js', function (file) {
        var arr = slash(file.path).split('/');
        arr.pop(); // remove file name
        moduleName = arr.pop();
        if (moduleName === 'view') {
            moduleName = arr.pop();
        }
        gulp.start('build-config', function () {
            gutil.log('Generate ' + gutil.colors.yellow('config/' + moduleName + '.js'));
        });
    });
    gulp.watch('./apps/**/*.js', function (file) {
        var arr = slash(file.path).split('/');
        arr.pop(); // remove file name
        moduleName = arr.pop();
        if (moduleName === 'view') {
            moduleName = arr.pop();
        }
        gulp.start('build-app', function () {
            gutil.log('Generate ' + gutil.colors.yellow('apps/' + moduleName + '.js'));
        });
    });
});
