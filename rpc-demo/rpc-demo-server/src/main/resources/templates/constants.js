"use strict";
(function () {
    'use strict';
    // Do NOT edit this file
    angular
        .module('smartcloudserviceApp')
        .constant('APP_NAME', '@project.name@')
        .constant('VERSION', '@project.version@')
        .constant('COMPANY_NAME', 'Infinity Organization')
        .constant('PAGINATION_CONSTANTS', {
            'itemsPerPage': 10
        })
        //        .constant('ENV', '@spring.profiles.active@')
        .constant('DEBUG_INFO_ENABLED', true);
})();