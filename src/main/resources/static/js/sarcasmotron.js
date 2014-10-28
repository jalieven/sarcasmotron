var sarcasmotron = angular.module('sarcasmotron', ['ngRoute', 'sarcasmotron.services']);

var controllers = {};

controllers.HomeController = function ($scope) {

};

controllers.NewController = function ($scope) {

};

controllers.StreamController = function ($scope, sarcasms) {

};

controllers.StatsController = function ($scope) {

};

controllers.CommentController = function ($scope, sarcasm) {

};

sarcasmotron.controller(controllers);

sarcasmotron.config(function ($routeProvider, $httpProvider) {

    $routeProvider
        .when('/',
        {
            controller: 'HomeController',
            templateUrl: 'partial/home.html'
        })
        .when('/new',
        {
            controller: 'NewController',
            templateUrl: 'partial/new.html'
        })
        .when('/stream',
        {
            controller: 'StreamController',
            templateUrl: 'partial/stream.html',
            resolve: {
                sarcasms: function (SarcasmsLoader) {
                    return SarcasmsLoader();
                }
            }
        })
        .when('/stats',
        {
            controller: 'StatsController',
            templateUrl: 'partial/stats.html'
        })
        .when('/sarcasm/:sarcasmId/comment',
        {
            controller: 'CommentController',
            templateUrl: 'partial/comment.html',
            resolve: {
                sarcasm: function (SarcasmLoader) {
                    return SarcasmLoader();
                }
            }
        })
        .otherwise({ redirectTo: '/'});

//    $httpProvider.responseInterceptors.push(notificationInterceptor);

});