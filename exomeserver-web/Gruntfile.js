/*global module:false*/
module.exports = function (grunt) {

    // Project configuration.
    grunt.initConfig({
        // Metadata.
        pkg: grunt.file.readJSON('package.json'),
        jsopkg: grunt.file.readJSON('lib/jsorolla/package.json'),
        def: {
            name: 'exome-server',
            build: 'build/<%= pkg.version %>',
            jsorolla: 'lib/jsorolla'
        },
        // Task configuration.
        concat: {
            dist: {
                src: [
                    'src/exome-server-widget.js',
                    'src/exome-server.js'
                ],
                dest: '<%= def.build %>/<%= def.name %>.js'
            }
        },
        uglify: {
            options: {
                banner: '/*! <%= def.name %> <%= grunt.template.today("dd-mm-yyyy") %> */\n'

            },
            dist: {
                files: {
                    '<%= def.build %>/<%= def.name %>.min.js': ['<%= concat.dist.dest %>']
                }
            }
        },

        copy: {
            build: {
                files: [
                    {   expand: true, cwd: './src', src: ['es-config.js'], dest: '<%= def.build %>' },
                    {   expand: true, cwd: './<%= def.jsorolla %>', src: ['vendor/**'], dest: '<%= def.build %>' },
                    {   expand: true, cwd: './<%= def.jsorolla %>', src: ['styles/**'], dest: '<%= def.build %>' }, // includes files in path and its subdirs
                    {   expand: true, cwd: './<%= def.jsorolla %>/build/<%= jsopkg.version %>/genome-viewer', src: ['genome-viewer*.js', 'gv-config.js'], dest: '<%= def.build %>/' },
                    {   expand: true, cwd: './<%= def.jsorolla %>', src: ['worker*'], dest: '<%= def.build %>/' }
                ]
            }
        },
        clean: {
            build: ["<%= def.build %>/"]

        },

        htmlbuild: {
            dist: {
                src: 'src/<%= def.name %>.html',
                dest: '<%= def.build %>',
                options: {
                    beautify: true,
                    scripts: {
                        'js': '<%= def.build %>/<%= def.name %>.min.js',
                        'vendor': [
                            '<%= def.build %>/vendor/jquery.min.js',
                            '<%= def.build %>/vendor/underscore*.js',
                            '<%= def.build %>/vendor/backbone*.js',
                            '<%= def.build %>/vendor/bootstrap-scoped-dist/js/bootstrap.min.js',
                            '<%= def.build %>/vendor/typeahead.min.js',
                            '<%= def.build %>/vendor/jquery.mousewheel*.js',
                            '<%= def.build %>/vendor/gl-matrix-min*.js',
                            '<%= def.build %>/vendor/ChemDoodleWeb*.js',
                            '<%= def.build %>/vendor/jquery.cookie*.js',
                            '<%= def.build %>/vendor/purl*.js',
                            '<%= def.build %>/vendor/jquery.sha1*.js',
                            '<%= def.build %>/vendor/jquery.qtip*.js',
                            '<%= def.build %>/vendor/rawdeflate*.js',

                        ],
                        gv: [
                            '<%= def.build %>/opencga*.min.js',
                            '<%= def.build %>/genome-viewer*.min.js'
                        ],
                        gvconfig: [
                            '<%= def.build %>/gv-config.js'
                        ]
                    },
                    styles: {
                        'css': ['<%= def.build %>/styles/css/style.css'],
                        'vendor': [
                            '<%= def.build %>/vendor/ChemDoodleWeb*.css',
                            '<%= def.build %>/vendor/jquery.qtip*.css',
                            '<%= def.build %>/vendor/bootstrap-scoped-dist/css/bootstrap.min.css',
                            '<%= def.build %>/vendor/typeahead.js-bootstrap.css'
                        ]
                    }
                }
            }
        },
        rename: {
            html: {
                files: [
                    {
                        src: ['<%= def.build %>/<%= def.name %>.html'],
                        dest: '<%= def.build %>/index.html'}
                ]
            }
        },

        hub: {
            'genome-viewer': {
                src: ['lib/jsorolla/Gruntfile.js'],
                tasks: ['gv']
            }
        }

    });

    // These plugins provide necessary tasks.
    grunt.loadNpmTasks('grunt-contrib-concat');
    grunt.loadNpmTasks('grunt-contrib-uglify');
    grunt.loadNpmTasks('grunt-contrib-watch');
    grunt.loadNpmTasks('grunt-contrib-copy');
    grunt.loadNpmTasks('grunt-contrib-clean');
    grunt.loadNpmTasks('grunt-contrib-rename');
    grunt.loadNpmTasks('grunt-html-build');
    grunt.loadNpmTasks('grunt-curl');
    grunt.loadNpmTasks('grunt-hub');

    grunt.registerTask('log-deploy', 'Deploy path info', function () {
        grunt.log.writeln("DEPLOY COMMAND: scp -r build/" + grunt.config.data.pkg.version + " cafetero@mem16:/httpd/bioinfo/www-apps/exome-server/");
    });

    // Default task.
    grunt.registerTask('default', ['hub', 'clean', 'concat', 'uglify', 'copy', 'htmlbuild', 'rename:html', 'log-deploy']);

};
