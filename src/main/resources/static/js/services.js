var services = angular.module('sarcasmotron.services', ['ngResource']);

services.factory('Sarcasm', function($resource) {
    return $resource('http://localhost:8080/sarcasms/:id',
        {
            get: {method: 'GET'},
            update: { method: 'PUT' },
            query:  {method:'GET', isArray:true}
        }
    );
});

services.factory('SarcasmsLoader', ['Sarcasm', '$q', function(Sarcasm, $q) {
    return function() {
        var delay = $q.defer();
        Sarcasm.query(function(sarcasms) {
            delay.resolve(sarcasms);
        }, function() {
            delay.reject('Unable to fetch sarcasms!');
        });
        return delay.promise;
    };
}]);

services.factory('SarcasmLoader', ['Sarcasm', '$route', '$q', function(Sarcasm, $route, $q) {
    return function() {
        var delay = $q.defer();
        Sarcasm.get({id: $route.current.params.sarcasmId}, function(sarcasm) {
            delay.resolve(sarcasm);
            delay.reject('Unable to fetch sarcasm ' + $route.current.params.sarcasmId);
        });
        return delay.promise;
    };
}]);