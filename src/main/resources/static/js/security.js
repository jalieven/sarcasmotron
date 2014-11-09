document.domain = 'mmis.be';

var getSSOToken = function() {
    return $.cookie("openamssoid");
}

var hasCookieAndValid = function(){
    var valid = false;
    var ssoCookie = $.cookie("openamssoid");
    if (ssoCookie != null) {
        $.ajax({url: "http://local.mmis.be:1337/sso/isTokenValid?tokenid=" + ssoCookie,
            success: function (result, status, xhr) {
                console.log("Token valid result: " + result);
                valid = (result.replace(/(\r\n|\n|\r)/gm,"").split("=")[1].toLowerCase() === 'true');
            },
            error: function (xhr, result, error) {
                // could not validate!
                valid = false;
            }, async: false
        });
    }
    return valid;
};

var login = function(username, password, location) {
    var redirect = location || "/";
    $.ajax({url: "http://local.mmis.be:1337/sso/authenticate?username=" + username + "&password=" + password,
        success: function (result, status, xhr) {
            if (xhr.status == 200) {
                var ssoid = result.replace(/(\r\n|\n|\r)/gm,"").split("=")[1];
                $.cookie("openamssoid", ssoid, { path: '/', expires: 1 });
                document.location = redirect;
            } else {
                console.log("Authenticate result: " + result);
            }
        },
        error: function(xhr, status, error) {
            console.log("Authenticate result: " + error);
        },
        async: false
    });
};

var validateSecurity = function(){
    var ssoCookie = $.cookie("openamssoid");
    if (ssoCookie != null) {
        $.ajax({url: "http://local.mmis.be:1337/sso/isTokenValid?tokenid=" + ssoCookie,
            success: function (result, status, xhr) {
                // security is OK
            },
            error: function (xhr, result, error) {
                authenticateSilently();
            }
        });
    } else {
        authenticateSilently();
    }
};

var authenticateSilently = function() {
    $.ajax({url: "http://local.mmis.be:1337/sso/authenticate?username=" + username + "&password=" + password,
        success: function (result, status, xhr) {
            if (xhr.status == 200) {
                var ssoid = result.split("=")[1];
                $.cookie("openamssoid", ssoid, { path: '/', expires: 1 });
            } else {
                console.log("Authenticate result: " + result);
            }
        },
        error: function (xhr, result, error) {
            console.log("Authenticate result: " + error);
        }
    });
}