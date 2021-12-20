/**
 * Controllers
 */
angular
    .module('smartcloudserviceApp')
    .controller('MainController', MainController)
    .controller('LeftSidebarController', LeftSidebarController)
    .controller('DashboardController', DashboardController)
    .controller('ErrorPageController', ErrorPageController)
    .controller('LoginController', LoginController)
    .controller('NavbarController', NavbarController)
    .controller('FooterController', FooterController)
    .controller('ContactUsController', ContactUsController)
    .controller('ProfileController', ProfileController)
    .controller('RegisterController', RegisterController)
    .controller('ActivationController', ActivationController)
    .controller('ForgotPasswordController', ForgotPasswordController)
    .controller('ResetPasswordController', ResetPasswordController)
    .controller('PasswordController', PasswordController)
    .controller('MetricsController', MetricsController)
    .controller('MetricsDialogController', MetricsDialogController)
    .controller('HealthController', HealthController)
    .controller('HealthDialogController', HealthDialogController)
    .controller('ConfigurationController', ConfigurationController)
    .controller('BeansController', BeansController)
    .controller('BeanDialogController', BeanDialogController)
    .controller('MappingsController', MappingsController)
    .controller('HttpTraceController', HttpTraceController)
    .controller('HttpSessionController', HttpSessionController)
    .controller('AuditsController', AuditsController)
    .controller('TrackerController', TrackerController)
    .controller('DictListController', DictListController)
    .controller('DictDialogController', DictDialogController)
    .controller('DictItemListController', DictItemListController)
    .controller('DictItemDialogController', DictItemDialogController)
    .controller('LoggerController', LoggerController)
    .controller('ArthasController', ArthasController)
    .controller('ScheduleController', ScheduleController)
    .controller('ControlController', ControlController)
    .controller('RpcApplicationListController', RpcApplicationListController)
    .controller('RpcServerListController', RpcServerListController)
    .controller('RpcServerDetailsController', RpcServerDetailsController)
    .controller('RpcServiceListController', RpcServiceListController)
    .controller('RpcProviderListController', RpcProviderListController)
    .controller('RpcProviderDetailsController', RpcProviderDetailsController)
    .controller('RpcScheduledTaskDialogController', RpcScheduledTaskDialogController)
    .controller('RpcScheduledTaskListController', RpcScheduledTaskListController)
    .controller('RpcScheduledTaskHistoryListController', RpcScheduledTaskHistoryListController)
    .controller('RpcScheduledTaskHistoryDetailsController', RpcScheduledTaskHistoryDetailsController)
    .controller('RpcConsumerListController', RpcConsumerListController)
    .controller('AppListController', AppListController)
    .controller('AppDialogController', AppDialogController)
    .controller('AppDetailsController', AppDetailsController)
    .controller('AuthorityListController', AuthorityListController)
    .controller('AuthorityDialogController', AuthorityDialogController)
    .controller('OAuth2ClientListController', OAuth2ClientListController)
    .controller('OAuth2ClientDialogController', OAuth2ClientDialogController)
    .controller('OAuth2ClientDetailsController', OAuth2ClientDetailsController)
    .controller('OAuth2AccessTokenListController', OAuth2AccessTokenListController)
    .controller('OAuth2AccessTokenDetailsController', OAuth2AccessTokenDetailsController)
    .controller('OAuth2RefreshTokenListController', OAuth2RefreshTokenListController)
    .controller('OAuth2RefreshTokenDetailsController', OAuth2RefreshTokenDetailsController)
    .controller('OAuth2ApprovalListController', OAuth2ApprovalListController)
    .controller('OAuth2ApprovalDetailsController', OAuth2ApprovalDetailsController)
    .controller('AdminMenuListController', AdminMenuListController)
    .controller('AdminMenuDialogController', AdminMenuDialogController)
    .controller('AuthorityAdminMenuController', AuthorityAdminMenuController)
    .controller('UserListController', UserListController)
    .controller('UserDialogController', UserDialogController)
    .controller('UserDetailsController', UserDetailsController);

/**
 * MainController - controller
 * Contains several global data used in different view
 *
 */
function MainController($http, $rootScope, $scope, $state, AuthenticationService, PrincipalService, AuthorityAdminMenuService, AuthServerService, AlertUtils, APP_NAME, COMPANY_NAME, $localStorage) {
    var main = this;
    main.account = null;
    main.isAuthenticated = null;
    main.links = [];
    main.registries = [];
    $rootScope.selectedRegistryIdentity = null;
    main.selectedLink = null;
    main.selectLink = selectLink;
    $rootScope.companyName = COMPANY_NAME;

    loadRegistries();

    // Authenticate user whether has logged in
    AuthenticationService.authorize(false, getAccount);

    $scope.$on('authenticationSuccess', function () {
        getAccount();
    });

    $scope.$watch(PrincipalService.isAuthenticated, function () {
        loadLinks();
    });

    function loadLinks() {
        if (PrincipalService.isAuthenticated() == true) {
            main.links = AuthorityAdminMenuService.queryUserLinks({appName: APP_NAME});
        }
    }

    function getAccount() {
        PrincipalService.identity().then(function (account) {
            if (account == null) {
                return;
            }
            main.account = account;

            var authToken = AuthServerService.getToken();
            if (authToken) {
                main.account.profilePhotoUrl = '/api/accounts/profile-photo?access_token=' + authToken.access_token;
            }

            main.isAuthenticated = PrincipalService.isAuthenticated;

            // if (account) {
            //     AlertUtils.success('Sign in successfully');
            // }
        });
    }

    function loadRegistries() {
        $http.get('open-api/rpc-registries').then(function (response) {
            main.registries = response.data;
            if(main.registries) {
                $rootScope.selectedRegistryIdentity = main.registries[0].identity;
                $localStorage.selectedRegistryIdentity = $rootScope.selectedRegistryIdentity;
            }
        });
    }

    function selectLink($item, $model, $label, $event) {
        $state.go(main.selectedLink.url);
    }
}
/**
 * LeftSidebarController
 */
function LeftSidebarController($scope, $state, $element, $timeout, APP_NAME, AuthorityAdminMenuService, PrincipalService) {
    var vm = this;

    vm.init = init;
    vm.groups = [];

    $scope.$watch(PrincipalService.isAuthenticated, function () {
        vm.init();
    });

    function init() {
        if (PrincipalService.isAuthenticated() == true) {
            AuthorityAdminMenuService.queryUserMenus({appName: APP_NAME}, function (response) {
                if (response) {
                    vm.groups = response;
                    // Call the metsiMenu plugin and plug it to sidebar navigation
                    $timeout(function () {
                        $element.metisMenu();
                    });
                }
            }, function (errorResponse) {
            });
        }
    }
}
/**
 * DashboardController
 */
function DashboardController($http) {
    var vm = this;

    $http.get('api/rpc-statistics/data').then(function (response) {
        vm.data = response.data;
    });
}
/**
 * ErrorPageController
 */
function ErrorPageController($state, $stateParams, $scope, JSONFormatterConfig) {
    var vm = this;

    vm.errorMessage = $stateParams.errorMessage;
}
/**
 * LoginController
 */
function LoginController($rootScope, $state, AuthenticationService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.userName = 'louis';
    vm.password = 'louis';
    vm.errorMsg = null;
    vm.login = login;
    vm.requestResetPassword = requestResetPassword;
    vm.isSaving = false;

    function login(event) {
        event.preventDefault();
        vm.isSaving = true;
        AuthenticationService.login({
                userName: vm.userName,
                password: vm.password,
                rememberMe: vm.rememberMe
            },
            function (data) {
                vm.errorMsg = null;
//            if ($state.current.name === 'register' || $state.current.name === 'activate' ||
//                $state.current.name === 'finishReset' || $state.current.name === 'requestReset') {
//                $state.go('home');
//            }

                $rootScope.$broadcast('authenticationSuccess');

                // previousState was set in the authExpiredInterceptor before being redirected to login modal.
                // since login is successful, go to stored previousState and clear previousState
                if (AuthenticationService.getPreviousState()) {
                    var previousState = AuthenticationService.getPreviousState();
                    AuthenticationService.resetPreviousState();
                    $state.go(previousState.name, previousState.params);
                }

                $state.go('dashboard');
            },
            function (data) {
                vm.errorMsg = data.error_description;
                vm.isSaving = false;
            });
    }

    function requestResetPassword() {
        $state.go('requestReset');
    }
}
/**
 * NavbarController
 */
function NavbarController($rootScope, $scope, $translate, $state, AuthenticationService, PrincipalService, ProfileService) {
    var vm = this;

    vm.isNavbarCollapsed = true;
    vm.isAuthenticated = PrincipalService.isAuthenticated;
    vm.changeLanguage = changeLanguage;

    ProfileService.getProfileInfo().then(function (response) {
        vm.inProduction = response.inProduction;
        vm.swaggerEnabled = response.swaggerEnabled;
    });

    vm.logout = logout;
    vm.toggleNavbar = toggleNavbar;
    vm.collapseNavbar = collapseNavbar;
    vm.$state = $state;

    $rootScope.isNavbarLoaded = true;

    function changeLanguage(langKey) {
        $translate.use(langKey);
        $scope.language = langKey;
    }
    function logout() {
        AuthenticationService.logout();
        $state.go('login');
    }

    function toggleNavbar() {
        vm.isNavbarCollapsed = !vm.isNavbarCollapsed;
    }

    function collapseNavbar() {
        vm.isNavbarCollapsed = true;
    }
}

/**
 * FooterController
 */
function FooterController($http, PrincipalService) {
    var vm = this;

    PrincipalService.hasAuthority('ROLE_DEVELOPER')
        .then(function (result) {
            if (result) {
                $http({
                    url: 'api/systems/intranet-ip',
                    method: 'GET',
                    transformResponse: [function (data) {
                        return data;
                    }]
                }).then(function (response) {
                    vm.ip = response.data;
                });
            }
        });
}
/**
 * ContactUsController
 */
function ContactUsController($state) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
}
/**
 * ProfileController
 */
function ProfileController($state, PrincipalService, AccountService, AuthServerService, Upload) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.save = save;
    vm.isSaving = false;
    vm.profileAccount = null;
    vm.file = null;
    vm.uploading = false;
    vm.uploadProgress = 0;
    vm.upload = upload;

    var authToken = AuthServerService.getToken();
    if (authToken) {
        vm.profilePhotoUrl = '/api/accounts/profile-photo?access_token=' + authToken.access_token;
    }

    /**
     * Store the 'profile account' in a separate variable, and not in the shared 'account' variable.
     */
    var copyAccount = function (account) {
        return {
            id: account.id,
            activated: account.activated,
            email: account.email,
            mobileNo: parseInt(account.mobileNo),
            firstName: account.firstName,
            lastName: account.lastName,
            userName: account.userName,
            hasProfilePhoto: account.hasProfilePhoto
        };
    };

    PrincipalService.identity().then(function (account) {
        vm.profileAccount = copyAccount(account);
    });

    function save() {
        vm.isSaving = true;
        AccountService.update(vm.profileAccount,
            function (response) {
                vm.isSaving = false;
                PrincipalService.identity(true).then(function (account) {
                    vm.profileAccount = copyAccount(account);
                });
            },
            function (response) {
                vm.isSaving = false;
            });
    }

    function upload(file) {
        if (file) {
            vm.uploading = true;
            vm.uploadProgress = 30;
            Upload.upload({
                url: '/api/accounts/profile-photo/upload',
                // the 'file' must match the parameter of backend
                data: {file: file, description: "user profile"},
                disableProgress: false
            }).then(function (resp) {
                vm.uploadProgress = 100;
                vm.uploading = false;
            }, function (resp) {
                // if (resp.status > 0)
                //     vm.errorMsg = resp.status + ': ' + resp.data;
                vm.uploading = false;
            }, function (evt) {
                // does not work
                vm.uploadProgress = Math.min(100, parseInt(100.0 * evt.loaded / evt.total));
                vm.uploading = false;
            });
        }
    }
}
/**
 * RegisterController
 */
function RegisterController($state, $timeout, AuthenticationService, RegisterService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.register = register;
    vm.isSaving = false;
    vm.registerAccount = {};
    vm.passwordNotMatch = false;

    $timeout(function () {
        angular.element('#userName').focus();
    });

    function register() {
        if (vm.registerAccount.password !== vm.confirmPassword) {
            vm.passwordNotMatch = true;
        } else {
            vm.isSaving = true;
            RegisterService.create(vm.registerAccount,
                function (account) {
                    vm.isSaving = false;
                    vm.passwordNotMatch = false;
                    $state.go('login');
                },
                function (response) {
                    AuthenticationService.logout();
                    vm.isSaving = false;
                    vm.passwordNotMatch = false;
                });
        }
    }
}
/**
 * ActivationController
 */
function ActivationController($state, $stateParams, ActivateService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.success = false;
    vm.errorMessage = null;

    if ($stateParams.key) {
        ActivateService.get({key: $stateParams.key},
            function (response) {
                vm.success = true;
                vm.fieldErrors = [];
            },
            function (response, headers) {
                vm.success = false;
                vm.errorMessage = response.data.message;
            });
    }
}
/**
 * ForgotPasswordController
 */
function ForgotPasswordController($state, $timeout, PasswordResetInitService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.requestReset = requestReset;
    vm.email = '';
    vm.isSaving = false;
    vm.success = false;
    vm.errorMsg = null;

    $timeout(function () {
        angular.element('#email').focus();
    });

    function requestReset() {
        vm.isSaving = true;
        vm.success = false;
        PasswordResetInitService.create(vm.email,
            function (response) {
                vm.isSaving = false;
                vm.success = true;
            },
            function (response) {
                vm.isSaving = false;
                vm.success = false;
                vm.errorMsg = response.error_description;
            });
    }
}
/**
 * ResetPasswordController
 */
function ResetPasswordController($state, $stateParams, $timeout, PasswordResetFinishService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.resetPassword = resetPassword;
    vm.password = '';
    vm.confirmPassword = '';
    vm.success = false;
    vm.error = false;
    vm.isSaving = false;
    vm.keyMissing = angular.isUndefined($stateParams.key);

    $timeout(function () {
        angular.element('#password').focus();
    });

    function resetPassword() {
        vm.success = false;
        vm.error = false;
        vm.isSaving = true;
        PasswordResetFinishService.create({key: $stateParams.key, newPassword: vm.password},
            function (response) {
                vm.success = true;
                vm.isSaving = false;
            },
            function (response) {
                vm.success = false;
                vm.error = true;
                vm.isSaving = false;
            });
    }
}
/**
 * PasswordController
 */
function PasswordController($state, PasswordService, PrincipalService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.save = save;
    vm.isSaving = false;
    vm.passwordNotMatch = false;

    PrincipalService.identity().then(function (account) {
        vm.account = account;
    });

    function save() {
        if (vm.password !== vm.confirmPassword) {
            vm.passwordNotMatch = true;
        } else {
            vm.passwordNotMatch = false;
            vm.isSaving = true;
            PasswordService.update({'newPassword': vm.password},
                function (response) {
                    vm.isSaving = false;
                    $state.go('login');
                },
                function (response) {
                    vm.isSaving = false;
                });
        }
    }
}

/**
 * MetricsController
 *
 */
function MetricsController($state, $scope, $uibModal, MetricsService, metrics) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.cachesStats = {};
    vm.metrics = metrics;
    vm.refresh = refresh;
    vm.refreshThreadDumpData = refreshThreadDumpData;
    vm.servicesStats = {};
    vm.updatingMetrics = false;

    vm.markSweepCount = vm.metrics.gauges['jvm.garbage.PS-MarkSweep.count'] ?
        vm.metrics.gauges['jvm.garbage.PS-MarkSweep.count'].value :
        vm.metrics.gauges['jvm.garbage.Copy.count'].value;

    vm.markSweepTime = vm.metrics.gauges['jvm.garbage.PS-MarkSweep.time'] ?
        vm.metrics.gauges['jvm.garbage.PS-MarkSweep.time'].value :
        vm.metrics.gauges['jvm.garbage.Copy.time'].value;

    vm.scavengeCount = vm.metrics.gauges['jvm.garbage.PS-Scavenge.count'] ?
        vm.metrics.gauges['jvm.garbage.PS-Scavenge.count'].value :
        vm.metrics.gauges['jvm.garbage.MarkSweepCompact.count'].value;

    vm.scavengeTime = vm.metrics.gauges['jvm.garbage.PS-Scavenge.time'] ?
        vm.metrics.gauges['jvm.garbage.PS-Scavenge.time'].value :
        vm.metrics.gauges['jvm.garbage.MarkSweepCompact.time'].value;

    /**
     * Options for Doughnut chart
     */
    vm.doughnutOptions = {
        segmentShowStroke: true,
        segmentStrokeColor: '#fff',
        segmentStrokeWidth: 2,
        percentageInnerCutout: 45, // This is 0 for Pie charts
        animationSteps: 100,
        animationEasing: 'easeOutBounce',
        animateRotate: true,
        animateScale: false
    };

    vm.totalMemory = [
        {
            value: vm.metrics.gauges['jvm.memory.total.used'].value / 1000000,
            color: '#8d7fbf',
            highlight: '#4424bc',
            label: 'Used'
        },
        {
            value: (vm.metrics.gauges['jvm.memory.total.max'].value - vm.metrics.gauges['jvm.memory.total.used'].value) / 1000000,
            color: '#dedede',
            highlight: '#4424bc',
            label: 'Unused'
        }
    ];

    vm.heapMemory = [
        {
            value: vm.metrics.gauges['jvm.memory.heap.used'].value / 1000000,
            color: '#8d7fbf',
            highlight: '#4424bc',
            label: 'Used'
        },
        {
            value: (vm.metrics.gauges['jvm.memory.heap.max'].value - vm.metrics.gauges['jvm.memory.heap.used'].value) / 1000000,
            color: '#dedede',
            highlight: '#4424bc',
            label: 'Unused'
        }
    ];

    vm.edenSpaceUsed = vm.metrics.gauges['jvm.memory.pools.PS-Eden-Space.used'] ?
        vm.metrics.gauges['jvm.memory.pools.PS-Eden-Space.used'].value :
        vm.metrics.gauges['jvm.memory.pools.Eden-Space.used'].value;

    vm.edenSpaceMax = vm.metrics.gauges['jvm.memory.pools.PS-Eden-Space.max'] ?
        vm.metrics.gauges['jvm.memory.pools.PS-Eden-Space.max'].value :
        vm.metrics.gauges['jvm.memory.pools.Eden-Space.max'].value;
    vm.edenSpaceMemory = [
        {
            value: vm.edenSpaceUsed / 1000000,
            color: '#8d7fbf',
            highlight: '#4424bc',
            label: 'Used'
        },
        {
            value: (vm.edenSpaceMax - vm.edenSpaceUsed) / 1000000,
            color: '#dedede',
            highlight: '#4424bc',
            label: 'Unused'
        }
    ];

    vm.survivorSpaceUsed = vm.metrics.gauges['jvm.memory.pools.PS-Survivor-Space.used'] ?
        vm.metrics.gauges['jvm.memory.pools.PS-Survivor-Space.used'].value :
        vm.metrics.gauges['jvm.memory.pools.Survivor-Space.used'].value;

    vm.survivorSpaceMax = vm.metrics.gauges['jvm.memory.pools.PS-Survivor-Space.max'] ?
        vm.metrics.gauges['jvm.memory.pools.PS-Survivor-Space.max'].value :
        vm.metrics.gauges['jvm.memory.pools.Survivor-Space.max'].value;
    vm.survivorSpaceMemory = [
        {
            value: vm.survivorSpaceUsed / 1000000,
            color: '#8d7fbf',
            highlight: '#4424bc',
            label: 'Used'
        },
        {
            value: (vm.survivorSpaceMax - vm.survivorSpaceUsed) / 1000000,
            color: '#dedede',
            highlight: '#4424bc',
            label: 'Unused'
        }
    ];

    vm.oldGenUsed = vm.metrics.gauges['jvm.memory.pools.PS-Old-Gen.used'] ?
        vm.metrics.gauges['jvm.memory.pools.PS-Old-Gen.used'].value :
        vm.metrics.gauges['jvm.memory.pools.Tenured-Gen.used'].value;

    vm.oldGenMax = vm.metrics.gauges['jvm.memory.pools.PS-Old-Gen.max'] ?
        vm.metrics.gauges['jvm.memory.pools.PS-Old-Gen.max'].value :
        vm.metrics.gauges['jvm.memory.pools.Tenured-Gen.max'].value;
    vm.oldSpaceMemory = [
        {
            value: vm.oldGenUsed / 1000000,
            color: '#8d7fbf',
            highlight: '#4424bc',
            label: 'Used'
        },
        {
            value: (vm.oldGenMax - vm.oldGenUsed) / 1000000,
            color: '#dedede',
            highlight: '#4424bc',
            label: 'Unused'
        }
    ];

    vm.nonHeapMemory = [
        {
            value: vm.metrics.gauges['jvm.memory.non-heap.used'].value / 1000000,
            color: '#8d7fbf',
            highlight: '#4424bc',
            label: 'Used'
        },
        {
            value: (vm.metrics.gauges['jvm.memory.non-heap.committed'].value - vm.metrics.gauges['jvm.memory.non-heap.used'].value) / 1000000,
            color: '#dedede',
            highlight: '#4424bc',
            label: 'Unused'
        }
    ];

    vm.runnableThreads = [
        {
            value: vm.metrics.gauges['jvm.threads.runnable.count'].value,
            color: '#8d7fbf',
            highlight: '#4424bc',
            label: 'Runnable'
        },
        {
            value: (vm.metrics.gauges['jvm.threads.count'].value - vm.metrics.gauges['jvm.threads.runnable.count'].value),
            color: '#dedede',
            highlight: '#4424bc',
            label: 'Others'
        }
    ];

    vm.timedWaitingThreads = [
        {
            value: vm.metrics.gauges['jvm.threads.timed_waiting.count'].value,
            color: '#8d7fbf',
            highlight: '#4424bc',
            label: 'Timed waiting'
        },
        {
            value: (vm.metrics.gauges['jvm.threads.count'].value - vm.metrics.gauges['jvm.threads.timed_waiting.count'].value),
            color: '#dedede',
            highlight: '#4424bc',
            label: 'Others'
        }
    ];

    vm.waitingThreads = [
        {
            value: vm.metrics.gauges['jvm.threads.waiting.count'].value,
            color: '#8d7fbf',
            highlight: '#4424bc',
            label: 'Waiting'
        },
        {
            value: (vm.metrics.gauges['jvm.threads.count'].value - vm.metrics.gauges['jvm.threads.waiting.count'].value),
            color: '#dedede',
            highlight: '#4424bc',
            label: 'Others'
        }
    ];

    vm.blockedThreads = [
        {
            value: vm.metrics.gauges['jvm.threads.blocked.count'].value,
            color: '#8d7fbf',
            highlight: '#4424bc',
            label: 'Blocked'
        },
        {
            value: (vm.metrics.gauges['jvm.threads.count'].value - vm.metrics.gauges['jvm.threads.blocked.count'].value),
            color: '#dedede',
            highlight: '#4424bc',
            label: 'Others'
        }
    ];

    $scope.$watch('vm.metrics', function (newValue) {
        vm.servicesStats = {};
        vm.cachesStats = {};
        angular.forEach(newValue.timers, function (value, key) {
            if (key.indexOf('controller.') !== -1) {
                var controllerStartIndex = key.indexOf('controller.');
                var offset = 'controller.'.length;
                vm.servicesStats[key.substr(controllerStartIndex + offset, key.length)] = value;
            }
            if (key.indexOf('service.impl.') !== -1) {
                var controllerStartIndex = key.indexOf('service.impl.');
                var offset = 'service.impl.'.length;
                vm.servicesStats[key.substr(controllerStartIndex + offset, key.length)] = value;
            }
            if (key.indexOf('net.sf.ehcache.Cache') !== -1) {
                // remove gets or puts
                var index = key.lastIndexOf('.');
                var newKey = key.substr(0, index);

                // Keep the name of the domain
                index = newKey.lastIndexOf('.');
                vm.cachesStats[newKey] = {
                    'name': newKey.substr(index + 1),
                    'value': value
                };
            }
        });
    });

    function refresh() {
        vm.updatingMetrics = true;
        MetricsService.getMetrics().then(function (promise) {
            vm.metrics = promise;
            vm.updatingMetrics = false;
        }, function (promise) {
            vm.metrics = promise.data;
            vm.updatingMetrics = false;
        });
    }

    function refreshThreadDumpData() {
        MetricsService.threadDump().then(function (data) {
            $uibModal.open({
                templateUrl: 'app/views/developer/metrics/metrics.dialog.html',
                controller: 'MetricsDialogController',
                controllerAs: 'vm',
                size: 'lg',
                resolve: {
                    threadDump: function () {
                        return data.threads;
                    }
                }
            });
        });
    }
}

/**
 * MetricsDialogController
 *
 */
function MetricsDialogController($uibModalInstance, threadDump) {
    var vm = this;

    vm.cancel = cancel;
    vm.getLabelClass = getLabelClass;
    vm.threadDump = threadDump;
    vm.threadDumpAll = 0;
    vm.threadDumpBlocked = 0;
    vm.threadDumpRunnable = 0;
    vm.threadDumpTimedWaiting = 0;
    vm.threadDumpWaiting = 0;

    angular.forEach(threadDump, function (value) {
        if (value.threadState === 'RUNNABLE') {
            vm.threadDumpRunnable += 1;
        } else if (value.threadState === 'WAITING') {
            vm.threadDumpWaiting += 1;
        } else if (value.threadState === 'TIMED_WAITING') {
            vm.threadDumpTimedWaiting += 1;
        } else if (value.threadState === 'BLOCKED') {
            vm.threadDumpBlocked += 1;
        }
    });

    vm.threadDumpAll = vm.threadDumpRunnable + vm.threadDumpWaiting +
        vm.threadDumpTimedWaiting + vm.threadDumpBlocked;

    function cancel() {
        $uibModalInstance.dismiss('cancel');
    }

    function getLabelClass(threadState) {
        if (threadState === 'RUNNABLE') {
            return 'label-success';
        } else if (threadState === 'WAITING') {
            return 'label-info';
        } else if (threadState === 'TIMED_WAITING') {
            return 'label-warning';
        } else if (threadState === 'BLOCKED') {
            return 'label-danger';
        }
    }
}

function HealthController($state, HealthService, $uibModal) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.updatingHealth = true;
    vm.getLabelClass = getLabelClass;
    vm.refresh = refresh;
    vm.showHealth = showHealth;
    vm.baseName = HealthService.getBaseName;
    vm.subSystemName = HealthService.getSubSystemName;

    vm.refresh();

    function getLabelClass(statusState) {
        if (statusState === 'UP') {
            return 'label-primary';
        } else {
            return 'label-danger';
        }
    }

    function refresh() {
        vm.updatingHealth = true;
        HealthService.checkHealth().then(function (response) {
            vm.healthData = HealthService.transformHealthData(response);
            vm.updatingHealth = false;
        }, function (response) {
            vm.healthData = HealthService.transformHealthData(response.data);
            vm.updatingHealth = false;
        });
    }

    function showHealth(health) {
        $uibModal.open({
            templateUrl: 'app/views/developer/health/health.dialog.html',
            controller: 'HealthDialogController',
            controllerAs: 'vm',
            size: 'lg',
            resolve: {
                currentHealth: function () {
                    return health;
                },
                baseName: function () {
                    return vm.baseName;
                },
                subSystemName: function () {
                    return vm.subSystemName;
                }
            }
        });
    }
}

function HealthDialogController($uibModalInstance, currentHealth, baseName, subSystemName) {
    var vm = this;

    vm.cancel = cancel;
    vm.currentHealth = currentHealth;
    vm.baseName = baseName;
    vm.subSystemName = subSystemName;

    function cancel() {
        $uibModalInstance.dismiss('cancel');
    }
}

function ConfigurationController($state, ConfigurationService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.allConfiguration = null;
    vm.configuration = null;
    vm.configKeys = [];

    ConfigurationService.get().then(function (configuration) {
        vm.configuration = configuration;

        for (var config in configuration) {
            if (config.properties !== undefined) {
                vm.configKeys.push(Object.keys(config.properties));
            }
        }
    });
    ConfigurationService.getEnv().then(function (configuration) {
        vm.allConfiguration = configuration;
    });
}

function BeansController($state, $http, $uibModal, APP_NAME) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.items = null;
    vm.refresh = refresh;
    vm.showBean = showBean;
    vm.refresh();

    function refresh() {
        $http.get('management/beans').then(function (response) {
            vm.items = [];
            angular.forEach(response.data['contexts'][APP_NAME]['beans'], function (val, key) {
                vm.items.push({bean: key, type: val.type, scope: val.scope, dependencies: val.dependencies});
            });
        });
    }

    function showBean(name) {
        $uibModal.open({
            templateUrl: 'app/views/developer/beans/bean.dialog.html',
            controller: 'BeanDialogController',
            controllerAs: 'vm',
            size: 'lg',
            resolve: {
                name: function () {
                    return name;
                },
                beanDetails: function () {
                    return $http.get('api/systems/bean', {
                        params: {
                            'name': name
                        }
                    }).then(function (response) {
                        return response.data;
                    });
                }
            }
        });
    }
}

function BeanDialogController($uibModalInstance, name, beanDetails) {
    var vm = this;

    vm.cancel = cancel;
    vm.name = name;
    vm.beanDetails = beanDetails;

    function cancel() {
        $uibModalInstance.dismiss('cancel');
    }
}

function MappingsController($state, $http, APP_NAME) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.items = [];
    vm.refresh = refresh;
    vm.refresh();

    function refresh() {
        $http.get('management/mappings').then(function (response) {
            var mappings = response.data['contexts'][APP_NAME]['mappings'];

            for (var key in mappings) {
                if (key === 'dispatcherServlets') {
                    angular.forEach(mappings[key]['dispatcherServlet'], function (v, k) {
                        vm.items.push({url: v.predicate, handler: v.handler});
                    });
                } else if (key === 'servletFilters') {
                    angular.forEach(mappings[key], function (v, k) {
                        vm.items.push({url: v.urlPatternMappings, handler: v.className});
                    });
                } else if (key === 'servlets') {
                    angular.forEach(mappings[key], function (v, k) {
                        vm.items.push({url: v.mappings, handler: v.className});
                    });
                }
            }
        });
    }
}

function HttpTraceController($state, $http) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.refresh = refresh;
    vm.refresh();

    function refresh() {
        $http.get('management/httptrace').then(function (response) {
            vm.items = response.data.traces;
        });
    }
}

/**
 * HttpSessionController
 */
function HttpSessionController($state, AlertUtils, ParseLinksUtils, PAGINATION_CONSTANTS, pagingParams, criteria, HttpSessionService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.links = null;
    vm.loadAll = loadAll;
    vm.loadPage = loadPage;
    vm.checkPressEnter = checkPressEnter;
    vm.page = 1;
    vm.totalItems = null;
    vm.entities = [];
    vm.predicate = pagingParams.predicate;
    vm.reverse = pagingParams.ascending;
    vm.itemsPerPage = PAGINATION_CONSTANTS.itemsPerPage;
    vm.transition = transition;
    vm.criteria = criteria;
    vm.del = del;

    vm.loadAll();

    function loadAll() {
        HttpSessionService.query({
            page: pagingParams.page - 1,
            size: vm.itemsPerPage,
            sort: sort(),
            principal: vm.criteria.principal
        }, function (result, headers) {
            vm.links = ParseLinksUtils.parse(headers('link'));
            vm.totalItems = headers('X-Total-Count');
            vm.page = pagingParams.page;
            vm.entities = result;
        });
    }

    function sort() {
        var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
        if (vm.predicate !== 'principal') {
            // default sort column
            result.push('principal,asc');
        }
        return result;
    }

    function loadPage(page) {
        vm.page = page;
        vm.transition();
    }

    function transition() {
        $state.transitionTo($state.$current, {
            page: vm.page,
            sort: vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc'),
            principal: vm.criteria.principal
        });
    }

    function checkPressEnter($event) {
        //按下enter键重新查询数据
        if ($event.keyCode == 13) {
            vm.transition();
        }
    }

    function del(id) {
        AlertUtils.createDeleteConfirmation('The data may be referenced by other data, and there may be some problems after deletion, are you sure to delete?', function (isConfirm) {
            if (isConfirm) {
                HttpSessionService.del({id: id},
                    function () {
                        vm.loadAll();
                    },
                    function () {
                    });
            }
        });
    }
}

function AuditsController($state, $filter, AuditsService, ParseLinksUtils, PAGINATION_CONSTANTS) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.entities = null;
    vm.links = null;
    vm.loadAll = loadAll;
    vm.page = 1;
    vm.itemsPerPage = 20;
    vm.previousMonth = previousMonth;
    // Previous 1 month
    vm.fromDate = null;
    // Tomorrow
    vm.toDate = null;
    vm.today = today;
    vm.totalItems = null;
    vm.predicate = 'auditEventDate';
    vm.reverse = false;

    vm.today();
    vm.previousMonth();
    vm.loadAll();

    function loadAll() {
        var dateFormat = 'yyyy-MM-dd';
        var fromDate = $filter('date')(vm.fromDate, dateFormat);
        var toDate = $filter('date')(vm.toDate, dateFormat);
        AuditsService.query({
            page: vm.page - 1,
            size: vm.itemsPerPage,
            sort: [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')],
            from: fromDate,
            to: toDate
        }, function (result, headers) {
            vm.links = ParseLinksUtils.parse(headers('link'));
            vm.totalItems = headers('X-Total-Count');
            vm.entities = result;
        });
    }

    // Date picker configuration
    function today() {
        // Today + 1 day - needed if the current day must be included
        var today = new Date();
        vm.toDate = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 1);
    }

    function previousMonth() {
        var fromDate = new Date();
        if (fromDate.getMonth() === 0) {
            fromDate = new Date(fromDate.getFullYear() - 1, 11, fromDate.getDate());
        } else {
            fromDate = new Date(fromDate.getFullYear(), fromDate.getMonth() - 1, fromDate.getDate());
        }
        vm.fromDate = fromDate;
    }
}

/**
 * TrackerController
 */
function TrackerController($cookies, $http, TrackerService) {
    // This controller uses a Websocket connection to receive user activities in real-time.
    var vm = this;

    vm.activities = [];

    TrackerService.receive().then(null, null, function (activity) {
        showActivity(activity);
    });

    function showActivity(activity) {
        var existingActivity = false;
        for (var index = 0; index < vm.activities.length; index++) {
            if (vm.activities[index].sessionId === activity.sessionId) {
                existingActivity = true;
                if (activity.page === 'logout') {
                    vm.activities.splice(index, 1);
                } else {
                    vm.activities[index] = activity;
                }
            }
        }
        if (!existingActivity && (activity.page !== 'logout')) {
            vm.activities.push(activity);
        }
    }

}

/**
 * DictListController
 */
function DictListController($state, AlertUtils, ParseLinksUtils, PAGINATION_CONSTANTS, pagingParams, criteria, DictService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.links = null;
    vm.loadAll = loadAll;
    vm.loadPage = loadPage;
    vm.checkPressEnter = checkPressEnter;
    vm.page = 1;
    vm.setEnabled = setEnabled;
    vm.totalItems = null;
    vm.entities = [];
    vm.predicate = pagingParams.predicate;
    vm.reverse = pagingParams.ascending;
    vm.itemsPerPage = PAGINATION_CONSTANTS.itemsPerPage;
    vm.transition = transition;
    vm.criteria = criteria;
    vm.del = del;

    vm.loadAll();

    function loadAll() {
        DictService.query({
            page: pagingParams.page - 1,
            size: vm.itemsPerPage,
            sort: sort(),
            dictName: vm.criteria.dictName
        }, function (result, headers) {
            vm.links = ParseLinksUtils.parse(headers('link'));
            vm.totalItems = headers('X-Total-Count');
            vm.page = pagingParams.page;
            vm.entities = result;
        });
    }

    function sort() {
        var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
        if (vm.predicate !== 'dictCode') {
            // default sort column
            result.push('dictCode,asc');
        }
        return result;
    }

    function loadPage(page) {
        vm.page = page;
        vm.transition();
    }

    function transition() {
        $state.transitionTo($state.$current, {
            page: vm.page,
            sort: vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc'),
            dictName: vm.criteria.dictName
        });
    }

    function checkPressEnter($event) {
        //按下enter键重新查询数据
        if ($event.keyCode == 13) {
            vm.transition();
        }
    }

    function setEnabled(entity, enabled) {
        entity.enabled = enabled;
        DictService.update(entity,
            function () {
                vm.loadAll();
            },
            function () {
                entity.enabled = !enabled;
            });
    }

    function del(id) {
        AlertUtils.createDeleteConfirmation('The data may be referenced by other data, and there may be some problems after deletion, are you sure to delete?', function (isConfirm) {
            if (isConfirm) {
                DictService.del({id: id},
                    function () {
                        vm.loadAll();
                    },
                    function () {
                    });
            }
        });
    }
}

/**
 * DictDialogController
 */
function DictDialogController($state, $stateParams, $uibModalInstance, DictService, entity) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.mode = $state.current.data.mode;
    vm.entity = entity;
    vm.isSaving = false;
    vm.save = save;
    vm.cancel = cancel;

    function save() {
        vm.isSaving = true;
        if (vm.mode == 'edit') {
            DictService.update(vm.entity, onSaveSuccess, onSaveError);
        } else {
            DictService.create(vm.entity, onSaveSuccess, onSaveError);
        }
    }

    function onSaveSuccess(result) {
        vm.isSaving = false;
        $uibModalInstance.close(result);
    }

    function onSaveError(result) {
        vm.isSaving = false;
    }

    function cancel() {
        $uibModalInstance.dismiss('cancel');
    }
}

/**
 * DictItemListController
 */
function DictItemListController($state, AlertUtils, ParseLinksUtils, PAGINATION_CONSTANTS, pagingParams, criteria, DictService, DictItemService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.dicts = DictService.query();
    vm.links = null;
    vm.loadAll = loadAll;
    vm.loadPage = loadPage;
    vm.checkPressEnter = checkPressEnter;
    vm.page = 1;
    vm.setEnabled = setEnabled;
    vm.totalItems = null;
    vm.entities = [];
    vm.predicate = pagingParams.predicate;
    vm.reverse = pagingParams.ascending;
    vm.itemsPerPage = PAGINATION_CONSTANTS.itemsPerPage;
    vm.transition = transition;
    vm.criteria = criteria;
    vm.del = del;

    vm.loadAll();

    function loadAll() {
        DictItemService.query({
            page: pagingParams.page - 1,
            size: vm.itemsPerPage,
            sort: sort(),
            dictCode: vm.criteria.dictCode,
            dictItemName: vm.criteria.dictItemName
        }, function (result, headers) {
            vm.links = ParseLinksUtils.parse(headers('link'));
            vm.totalItems = headers('X-Total-Count');
            vm.page = pagingParams.page;
            vm.entities = result;
        });
    }

    function sort() {
        var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
        if (vm.predicate !== 'dictCode') {
            // default sort column
            result.push('dictCode,asc');
        }
        return result;
    }

    function loadPage(page) {
        vm.page = page;
        vm.transition();
    }

    function transition() {
        $state.transitionTo($state.$current, {
            page: vm.page,
            sort: vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc'),
            dictCode: vm.criteria.dictCode,
            dictItemName: vm.criteria.dictItemName
        });
    }

    function checkPressEnter($event) {
        //按下enter键重新查询数据
        if ($event.keyCode == 13) {
            vm.transition();
        }
    }

    function setEnabled(entity, enabled) {
        entity.enabled = enabled;
        DictItemService.update(entity,
            function () {
                vm.loadAll();
            },
            function () {
                entity.enabled = !enabled;
            });
    }

    function del(id) {
        AlertUtils.createDeleteConfirmation('The data may be referenced by other data, and there may be some problems after deletion, are you sure to delete?', function (isConfirm) {
            if (isConfirm) {
                DictItemService.del({id: id},
                    function () {
                        vm.loadAll();
                    },
                    function () {
                    });
            }
        });
    }
}

/**
 * DictItemDialogController
 */
function DictItemDialogController($state, $stateParams, $uibModalInstance, DictService, DictItemService, entity) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.dicts = DictService.query({enabled: true});
    vm.mode = $state.current.data.mode;
    vm.entity = entity;
    vm.isSaving = false;
    vm.save = save;
    vm.cancel = cancel;

    function save() {
        vm.isSaving = true;
        if (vm.mode == 'edit') {
            DictItemService.update(vm.entity, onSaveSuccess, onSaveError);
        } else {
            DictItemService.create(vm.entity, onSaveSuccess, onSaveError);
        }
    }

    function onSaveSuccess(result) {
        vm.isSaving = false;
        $uibModalInstance.close(result);
    }

    function onSaveError(result) {
        vm.isSaving = false;
    }

    function cancel() {
        $uibModalInstance.dismiss('cancel');
    }
}

/**
 * LoggerController
 */
function LoggerController($state, LoggerService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.changeLevel = changeLevel;
    vm.query = query;

    vm.query();

    function query() {
        LoggerService.query({}, function (data) {
            vm.loggers = [];
            angular.forEach(data.loggers, function (val, key) {
                vm.loggers.push({name: key, level: val.effectiveLevel});
            });
        });
    }

    function changeLevel(name, level) {
        // The first argument is the path variable, the second one is request body
        LoggerService.changeLevel({name: name}, {configuredLevel: level}, function () {
            vm.query();
        });
    }
}

/**
 * ArthasController
 */
function ArthasController(AuthServerService) {
    var vm = this;

    var authToken = AuthServerService.getToken();
    if (authToken) {
        vm.url = 'api/system/arthas-console?access_token=' + authToken.access_token;
    }
}

/**
 * ScheduleController
 */
function ScheduleController($state, $http) {
    var vm = this;
    vm.data = {};

    vm.pageTitle = $state.current.data.pageTitle;

    $http.get('management/scheduledtasks').then(function (response) {
        vm.data = response.data;
    });
}

/**
 * ControlController
 */
function ControlController($state, $http, AlertUtils) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.items = null;
    vm.shutdown = shutdown;

    function shutdown() {
        $http.post('management/shutdown').then(function (response) {
                AlertUtils.success('Shutdown successfully', {});
            },
            function (response) {
                AlertUtils.error('Shutdown failed', {});
            });
    }
}
/**
 * RpcApplicationListController
 */
function RpcApplicationListController($state, $rootScope, RpcApplicationService, RpcRegistryService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.parentPageTitle = $state.$current.parent.data.pageTitle;
    vm.entities = RpcApplicationService.query({registryIdentity: RpcRegistryService.getSelectedRegistryIdentity()});
}
/**
 * RpcServerListController
 */
function RpcServerListController($state, $rootScope, AlertUtils, ParseLinksUtils, PAGINATION_CONSTANTS, pagingParams, criteria, RpcServerService, RpcRegistryService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.parentPageTitle = $state.$current.parent.data.pageTitle;
    vm.links = null;
    vm.loadAll = loadAll;
    vm.loadPage = loadPage;
    vm.checkPressEnter = checkPressEnter;
    vm.page = 1;
    vm.totalItems = null;
    vm.entities = [];
    vm.predicate = pagingParams.predicate;
    vm.reverse = pagingParams.ascending;
    vm.itemsPerPage = PAGINATION_CONSTANTS.itemsPerPage;
    vm.transition = transition;
    vm.criteria = criteria;

    vm.loadAll();

    function loadAll() {
        RpcServerService.query({
            page: pagingParams.page - 1,
            size: vm.itemsPerPage,
            registryIdentity: RpcRegistryService.getSelectedRegistryIdentity(),
            address: vm.criteria.address,
            sort: sort()
        }, function (result, headers) {
            vm.links = ParseLinksUtils.parse(headers('link'));
            vm.totalItems = headers('X-Total-Count');
            vm.page = pagingParams.page;
            vm.entities = result;
        });
    }

    function sort() {
        var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
        if (vm.predicate !== 'application') {
            // default sort column
            result.push('application,asc');
        }
        return result;
    }

    function loadPage(page) {
        vm.page = page;
        vm.transition();
    }

    function transition() {
        $state.transitionTo($state.$current, {
            page: vm.page,
            sort: vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc'),
            application: vm.criteria.application,
            address: vm.criteria.address
        });
    }

    function checkPressEnter($event) {
        //按下enter键重新查询数据
        if ($event.keyCode == 13) {
            vm.transition();
        }
    }
}
/**
 * RpcServerDetailsController
 */
function RpcServerDetailsController($state, $stateParams, RpcServerService, entity) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.parentPageTitle = $state.$current.parent.data.pageTitle;
    vm.grandfatherPageTitle = $state.$current.parent.parent.data.pageTitle;
    vm.entity = entity;
    vm.refresh = refresh;

    function refresh() {
        RpcServerService.get({extension: $stateParams.id},
            function (response) {
                vm.entity = response;
            });
    }
}
/**
 * RpcServiceListController
 */
function RpcServiceListController($state, $rootScope, AlertUtils, ParseLinksUtils, PAGINATION_CONSTANTS, pagingParams, criteria, RpcServiceService, RpcRegistryService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.parentPageTitle = $state.$current.parent.data.pageTitle;
    vm.links = null;
    vm.loadAll = loadAll;
    vm.loadPage = loadPage;
    vm.checkPressEnter = checkPressEnter;
    vm.page = 1;
    vm.totalItems = null;
    vm.entities = [];
    vm.predicate = pagingParams.predicate;
    vm.reverse = pagingParams.ascending;
    vm.itemsPerPage = PAGINATION_CONSTANTS.itemsPerPage;
    vm.transition = transition;
    vm.criteria = criteria;
    vm.deactivate = deactivate;
    vm.activate = activate;

    vm.loadAll();

    function loadAll() {
        RpcServiceService.query({
            page: pagingParams.page - 1,
            size: vm.itemsPerPage,
            registryIdentity: RpcRegistryService.getSelectedRegistryIdentity(),
            interfaceName: vm.criteria.interfaceName,
            sort: sort()
        }, function (result, headers) {
            vm.links = ParseLinksUtils.parse(headers('link'));
            vm.totalItems = headers('X-Total-Count');
            vm.page = pagingParams.page;
            vm.entities = result;
        });
    }

    function sort() {
        var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
        if (vm.predicate !== 'application') {
            // default sort column
            result.push('application,asc');
        }
        return result;
    }

    function loadPage(page) {
        vm.page = page;
        vm.transition();
    }

    function transition() {
        $state.transitionTo($state.$current, {
            page: vm.page,
            sort: vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc'),
            interfaceName: vm.criteria.interfaceName
        });
    }

    function checkPressEnter($event) {
        //按下enter键重新查询数据
        if ($event.keyCode == 13) {
            vm.transition();
        }
    }

    function deactivate(entity) {
        RpcServiceService.deactivate({registryIdentity: RpcRegistryService.getSelectedRegistryIdentity(), interfaceName: entity.interfaceName},
            function (response) {
                vm.loadAll();
            });
    }

    function activate(entity) {
        RpcServiceService.activate({registryIdentity: RpcRegistryService.getSelectedRegistryIdentity(), interfaceName: entity.interfaceName},
            function (response) {
                vm.loadAll();
            });
    }
}
/**
 * RpcProviderListController
 */
function RpcProviderListController($state, $rootScope, AlertUtils, ParseLinksUtils, PAGINATION_CONSTANTS, pagingParams, criteria, RpcProviderService, RpcApplicationService, RpcRegistryService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.parentPageTitle = $state.$current.parent.data.pageTitle;
    vm.applications = RpcApplicationService.query({registryIdentity: RpcRegistryService.getSelectedRegistryIdentity(), extension: 'names'});
    vm.links = null;
    vm.loadAll = loadAll;
    vm.loadPage = loadPage;
    vm.checkPressEnter = checkPressEnter;
    vm.page = 1;
    vm.totalItems = null;
    vm.entities = [];
    vm.predicate = pagingParams.predicate;
    vm.reverse = pagingParams.ascending;
    vm.itemsPerPage = PAGINATION_CONSTANTS.itemsPerPage;
    vm.transition = transition;
    vm.criteria = criteria;

    vm.loadAll();

    function loadAll() {
        RpcProviderService.query({
            page: pagingParams.page - 1,
            size: vm.itemsPerPage,
            registryIdentity: RpcRegistryService.getSelectedRegistryIdentity(),
            application: vm.criteria.application,
            interfaceName: vm.criteria.interfaceName,
            address: vm.criteria.address,
            sort: sort()
        }, function (result, headers) {
            vm.links = ParseLinksUtils.parse(headers('link'));
            vm.totalItems = headers('X-Total-Count');
            vm.page = pagingParams.page;
            vm.entities = result;
        });
    }

    function sort() {
        var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
        if (vm.predicate !== 'application') {
            // default sort column
            result.push('application,asc');
        }
        return result;
    }

    function loadPage(page) {
        vm.page = page;
        vm.transition();
    }

    function transition() {
        $state.transitionTo($state.$current, {
            page: vm.page,
            sort: vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc'),
            application: vm.criteria.application,
            interfaceName: vm.criteria.interfaceName,
            address: vm.criteria.address
        });
    }

    function checkPressEnter($event) {
        //按下enter键重新查询数据
        if ($event.keyCode == 13) {
            vm.transition();
        }
    }
}
/**
 * RpcProviderDetailsController
 */
function RpcProviderDetailsController($state, $stateParams, $rootScope, $http, AlertUtils, entity, RpcServiceService, RpcProviderService, RpcScheduledTaskService, RpcRegistryService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.parentPageTitle = $state.$current.parent.data.pageTitle;
    vm.grandfatherPageTitle = $state.$current.parent.parent.data.pageTitle;
    vm.entity = entity;
    vm.argsHidden = true;
    vm.checkProgress = 0;
    if(vm.entity.active) {
        queryMethods();
    }
    vm.options = RpcProviderService.queryOptions({providerUrl: vm.entity.url});

    vm.selectMethod = selectMethod;
    vm.invoke = invoke;
    vm.checkHealth = checkHealth;
    vm.deactivate = deactivate;
    vm.activate = activate;
    vm.delTask = delTask;
    vm.saveOptions = saveOptions;
    vm.loadTasks = loadTasks;
    vm.selectTab = selectTab;
    vm.selectedMethodSignature = null;
    vm.selectedMethod = null;

    if('invocation' == $stateParams.tab) {
        vm.tabInvocation = true;
    } else if('health' == $stateParams.tab) {
        vm.tabHealth = true;
    } else if('tasks' == $stateParams.tab) {
        vm.tabTasks = true;
    } else {
        vm.tabConfigure = true;
    }

    vm.loadTasks();

    RpcServiceService.query({registryIdentity: RpcRegistryService.getSelectedRegistryIdentity(), interfaceName: vm.entity.interfaceName},
        function (response) {
            if(_.size(response) > 0) {
                vm.entity.consuming = response[0].consuming;
            }
        });

    function loadTasks() {
        vm.tasks = RpcScheduledTaskService.query({registryIdentity: RpcRegistryService.getSelectedRegistryIdentity(),
            interfaceName: vm.entity.interfaceName, form: vm.entity.form, version: vm.entity.version});
    }

    function queryMethods() {
        vm.methods = RpcProviderService.queryMethods({registryIdentity: RpcRegistryService.getSelectedRegistryIdentity(), providerUrl: vm.entity.url});
    }

    function selectMethod() {
        vm.result = undefined;
        vm.elasped = undefined;
        vm.args = [];

        if(!vm.selectedMethodSignature) {
            vm.argsHidden = true;
            return;
        }

        var filteredMethods = _.filter(vm.methods, function(m){ return m.methodSignature == vm.selectedMethodSignature; });
        if(!_.isEmpty(filteredMethods)) {
            vm.selectedMethod = filteredMethods[0];
            if(_.isEmpty(vm.selectedMethod.methodParamTypes)) {
                vm.argsHidden = true;
            } else {
                vm.argsHidden = false;
            }
        }
    }

    function invoke() {
        if(vm.selectedMethod) {
            vm.selectedMethod.registryIdentity = RpcRegistryService.getSelectedRegistryIdentity();
            vm.selectedMethod.providerUrl = vm.entity.url;
            vm.selectedMethod.args = [];
            angular.forEach(vm.args, function (val, key) {
                vm.selectedMethod.args.push(val);
            });

            $http.post('api/rpc-invocations/invoke', vm.selectedMethod).then(function(response) {
                vm.result = response.data;
                vm.elasped = response.headers('X-ELAPSED')
            });
        }
    }

    function checkHealth() {
        vm.checkProgress = 0;
        vm.healthMessage = '';
        vm.healthSuccess = false;
        vm.healthFailure = false;
        setTimeout(function(){ RpcProviderService.checkHealth({registryIdentity: RpcRegistryService.getSelectedRegistryIdentity(), providerUrl: vm.entity.url},
            function (response) {
                if('OK' == response.data) {
                    vm.checkProgress = 100;
                    vm.healthSuccess = true;
                } else {
                    vm.checkProgress = 100;
                    vm.healthFailure = true;
                    vm.healthMessage = response.data;
                }
            }); }, 200);
    }

    function deactivate(entity) {
        RpcProviderService.deactivate({registryIdentity: RpcRegistryService.getSelectedRegistryIdentity(), providerUrl: entity.url},
            function (response) {
                vm.entity.active = false;
            });
    }

    function activate(entity) {
        RpcProviderService.activate({registryIdentity: RpcRegistryService.getSelectedRegistryIdentity(), providerUrl: entity.url},
            function (response) {
                vm.entity.active = true;
                queryMethods();
            });
    }

    function delTask(id) {
        AlertUtils.createDeleteConfirmation('Are you sure to delete?', function (isConfirm) {
            if (isConfirm) {
                RpcScheduledTaskService.del({extension: id},
                    function () {
                        vm.loadTasks();
                    },
                    function () {
                    });
            }
        });
    }

    function saveOptions() {
        angular.forEach(vm.options, function (val, key) {
            if(val.type == 'Integer') {
                if(val.intValue != null && val.intValue >= 0) {
                    val.value = '' + val.intValue;
                } else if(val.intValue == null) {
                    val.value = '';
                }
            } else if(val.type == 'Boolean') {
                if(val.booleanValue != null && val.booleanValue) {
                    val.value = '' + val.booleanValue;
                } else if(val.booleanValue == null) {
                    val.value = '';
                }
            }
        });

        RpcProviderService.saveOptions({registryIdentity: RpcRegistryService.getSelectedRegistryIdentity(), url: entity.url, options: vm.options},
            function () {
                var tab;
                if(vm.tabInvocation) {
                    tab = 'invocation';
                } else if(vm.tabHealth) {
                    tab = 'health';
                } else if(vm.tabTasks) {
                    tab = 'tasks';
                } else {
                    tab = 'configure';
                }

                setTimeout(function(){
                    $state.transitionTo($state.$current, {
                        id: $stateParams.id,
                        tab: tab
                    }, {reload: true});
                }, 200);
            });
    }

    function selectTab() {
        var tab;
        if(vm.tabInvocation) {
            tab = 'invocation';
        } else if(vm.tabHealth) {
            tab = 'health';
        } else if(vm.tabTasks) {
            tab = 'tasks';
        } else {
            tab = 'configure';
        }
        $state.transitionTo($state.$current, {
            tab: tab,
            id: $stateParams.id
        });
    }
}

/**
 * RpcScheduledTaskDialogController
 */
function RpcScheduledTaskDialogController($rootScope, $state, $stateParams, $uibModalInstance, $filter, RpcScheduledTaskService, RpcProviderService, RpcRegistryService, entity) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.mode = $state.current.data.mode;
    vm.timeUnits = RpcScheduledTaskService.query({extension: 'time-units'});
    vm.faultTolerances = RpcScheduledTaskService.query({extension: 'fault-tolerances'});
    vm.entity = entity;
    vm.argsHidden = true;
    vm.selectedMethod = null;

    var dateFormat = 'yyyy-MM-dd HH:mm';
    if(vm.entity.startTime) {
        var startTimeStr = $filter('date')(vm.entity.startTime, dateFormat);
        vm.entity.startTime = new Date(startTimeStr);
    }
    if(vm.entity.stopTime) {
        var stopTimeStr = $filter('date')(vm.entity.stopTime, dateFormat);
        vm.entity.stopTime = new Date(stopTimeStr);
    }

    if (vm.mode == 'create') {
        RpcProviderService.get({extension: $stateParams.id},
            function (response) {
                vm.provider = response;
                vm.methods = RpcProviderService.queryMethods({registryIdentity: RpcRegistryService.getSelectedRegistryIdentity(), providerUrl: vm.provider.url});
            });
    }

    vm.isSaving = false;
    vm.selectMethod = selectMethod;
    vm.save = save;
    vm.cancel = cancel;

    function selectMethod() {
        vm.args = [];

        if(!vm.entity.methodSignature) {
            vm.argsHidden = true;
            return;
        }

        var filteredMethods = _.filter(vm.methods, function(m){ return m.methodSignature == vm.entity.methodSignature; });
        if(!_.isEmpty(filteredMethods)) {
            vm.selectedMethod = filteredMethods[0];
            if(_.isEmpty(vm.selectedMethod.methodParamTypes)) {
                vm.argsHidden = true;
            } else {
                vm.argsHidden = false;
            }
        }
    }

    function save() {
        vm.isSaving = true;
        if (vm.mode == 'edit') {
            RpcScheduledTaskService.update(vm.entity, onSaveSuccess, onSaveError);
        } else {
            if(vm.selectedMethod) {
                vm.entity.registryIdentity = RpcRegistryService.getSelectedRegistryIdentity();
                vm.entity.interfaceName = vm.provider.interfaceName;
                vm.entity.form = vm.provider.form;
                vm.entity.version = vm.provider.version;
                vm.entity.methodName = vm.selectedMethod.methodName;
                vm.entity.methodParamTypes = vm.selectedMethod.methodParamTypes;

                RpcScheduledTaskService.create(vm.entity, onSaveSuccess, onSaveError);
            }
        }
    }

    function onSaveSuccess(result) {
        vm.isSaving = false;
        $uibModalInstance.close(result);
    }

    function onSaveError(result) {
        vm.isSaving = false;
    }

    function cancel() {
        $uibModalInstance.dismiss('cancel');
    }
}
/**
 * RpcScheduledTaskListController
 */
function RpcScheduledTaskListController($rootScope, $state, AlertUtils, ParseLinksUtils, PAGINATION_CONSTANTS, pagingParams, criteria, RpcScheduledTaskService, RpcRegistryService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.links = null;
    vm.loadAll = loadAll;
    vm.loadPage = loadPage;
    vm.checkPressEnter = checkPressEnter;
    vm.page = 1;
    vm.setEnabled = setEnabled;
    vm.totalItems = null;
    vm.entities = [];
    vm.predicate = pagingParams.predicate;
    vm.reverse = pagingParams.ascending;
    vm.itemsPerPage = PAGINATION_CONSTANTS.itemsPerPage;
    vm.transition = transition;
    vm.criteria = criteria;
    vm.del = del;
    vm.goToHistory = goToHistory;

    vm.loadAll();

    function loadAll() {
        RpcScheduledTaskService.query({
            page: pagingParams.page - 1,
            size: vm.itemsPerPage,
            sort: sort(),
            registryIdentity: RpcRegistryService.getSelectedRegistryIdentity(),
            name: vm.criteria.name,
            interfaceName: vm.criteria.interfaceName
        }, function (result, headers) {
            vm.links = ParseLinksUtils.parse(headers('link'));
            vm.totalItems = headers('X-Total-Count');
            vm.page = pagingParams.page;
            vm.entities = result;
        });
    }

    function sort() {
        var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
        if (vm.predicate !== 'name') {
            // default sort column
            result.push('name,asc');
        }
        return result;
    }

    function loadPage(page) {
        vm.page = page;
        vm.transition();
    }

    function transition() {
        $state.transitionTo($state.$current, {
            page: vm.page,
            sort: vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc'),
            name: vm.criteria.name,
            interfaceName: vm.criteria.interfaceName
        });
    }

    function checkPressEnter($event) {
        //按下enter键重新查询数据
        if ($event.keyCode == 13) {
            vm.transition();
        }
    }

    function setEnabled(entity, enabled) {
        entity.enabled = enabled;
        RpcScheduledTaskService.update(entity,
            function () {
                vm.loadAll();
            },
            function () {
                entity.enabled = !enabled;
            });
    }

    function del(id) {
        AlertUtils.createDeleteConfirmation('Are you sure to delete?', function (isConfirm) {
            if (isConfirm) {
                RpcScheduledTaskService.del({extension: id},
                    function () {
                        vm.loadAll();
                    },
                    function () {
                    });
            }
        });
    }

    function goToHistory(name) {
        $state.go('rpc.scheduled-task-history-list', {'name': name});
    }
}
/**
 * RpcScheduledTaskHistoryListController
 */
function RpcScheduledTaskHistoryListController($rootScope, $state, AlertUtils, ParseLinksUtils, PAGINATION_CONSTANTS, pagingParams, criteria, RpcScheduledTaskHistoryService, RpcRegistryService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.links = null;
    vm.loadAll = loadAll;
    vm.loadPage = loadPage;
    vm.checkPressEnter = checkPressEnter;
    vm.page = 1;
    vm.totalItems = null;
    vm.entities = [];
    vm.predicate = pagingParams.predicate;
    vm.reverse = pagingParams.ascending;
    vm.itemsPerPage = PAGINATION_CONSTANTS.itemsPerPage;
    vm.transition = transition;
    vm.criteria = criteria;
    vm.goBack = goBack;

    vm.loadAll();

    function loadAll() {
        RpcScheduledTaskHistoryService.query({
            page: pagingParams.page - 1,
            size: vm.itemsPerPage,
            sort: sort(),
            registryIdentity: RpcRegistryService.getSelectedRegistryIdentity(),
            name: vm.criteria.name
        }, function (result, headers) {
            vm.links = ParseLinksUtils.parse(headers('link'));
            vm.totalItems = headers('X-Total-Count');
            vm.page = pagingParams.page;
            vm.entities = result;
        });
    }

    function sort() {
        var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
        if (vm.predicate !== 'name') {
            // default sort column
            result.push('name,asc');
        }
        return result;
    }

    function loadPage(page) {
        vm.page = page;
        vm.transition();
    }

    function transition() {
        $state.transitionTo($state.$current, {
            page: vm.page,
            sort: vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc'),
            name: vm.criteria.name,
            providerId: vm.criteria.providerId
        });
    }

    function checkPressEnter($event) {
        //按下enter键重新查询数据
        if ($event.keyCode == 13) {
            vm.transition();
        }
    }

    function goBack() {
        if(criteria.providerId) {
            $state.go('rpc.provider-list.view', {'id': criteria.providerId, 'tab': 'tasks'});
        } else {
            $state.go('rpc.scheduled-task-list');
        }
    }
}
/**
 * RpcScheduledTaskHistoryDetailsController
 */
function RpcScheduledTaskHistoryDetailsController($state, $stateParams, entity) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.parentPageTitle = $state.$current.parent.data.pageTitle;
    vm.grandfatherPageTitle = $state.$current.parent.parent.data.pageTitle;
    vm.entity = entity;
}
/**
 * RpcConsumerListController
 */
function RpcConsumerListController($state, $rootScope, AlertUtils, ParseLinksUtils, PAGINATION_CONSTANTS, pagingParams, criteria, RpcConsumerService, RpcApplicationService, RpcRegistryService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.parentPageTitle = $state.$current.parent.data.pageTitle;
    vm.applications = RpcApplicationService.query({registryIdentity: RpcRegistryService.getSelectedRegistryIdentity(), extension: 'names'});
    vm.links = null;
    vm.loadAll = loadAll;
    vm.loadPage = loadPage;
    vm.checkPressEnter = checkPressEnter;
    vm.page = 1;
    vm.totalItems = null;
    vm.entities = [];
    vm.predicate = pagingParams.predicate;
    vm.reverse = pagingParams.ascending;
    vm.itemsPerPage = PAGINATION_CONSTANTS.itemsPerPage;
    vm.transition = transition;
    vm.criteria = criteria;

    vm.loadAll();

    function loadAll() {
        RpcConsumerService.query({
            page: pagingParams.page - 1,
            size: vm.itemsPerPage,
            registryIdentity: RpcRegistryService.getSelectedRegistryIdentity(),
            application: vm.criteria.application,
            interfaceName: vm.criteria.interfaceName,
            address: vm.criteria.address,
            sort: sort()
        }, function (result, headers) {
            vm.links = ParseLinksUtils.parse(headers('link'));
            vm.totalItems = headers('X-Total-Count');
            vm.page = pagingParams.page;
            vm.entities = result;
        });
    }

    function sort() {
        var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
        if (vm.predicate !== 'application') {
            // default sort column
            result.push('application,asc');
        }
        return result;
    }

    function loadPage(page) {
        vm.page = page;
        vm.transition();
    }

    function transition() {
        $state.transitionTo($state.$current, {
            page: vm.page,
            sort: vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc'),
            application: vm.criteria.application,
            interfaceName: vm.criteria.interfaceName,
            address: vm.criteria.address
        });
    }

    function checkPressEnter($event) {
        //按下enter键重新查询数据
        if ($event.keyCode == 13) {
            vm.transition();
        }
    }
}
/**
 * AuthorityListController
 */
function AuthorityListController($state, AlertUtils, ParseLinksUtils, PAGINATION_CONSTANTS, pagingParams, criteria, AuthorityService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.parentPageTitle = $state.$current.parent.data.pageTitle;
    vm.links = null;
    vm.loadAll = loadAll;
    vm.loadPage = loadPage;
    vm.checkPressEnter = checkPressEnter;
    vm.page = 1;
    vm.setEnabled = setEnabled;
    vm.totalItems = null;
    vm.entities = [];
    vm.predicate = pagingParams.predicate;
    vm.reverse = pagingParams.ascending;
    vm.itemsPerPage = PAGINATION_CONSTANTS.itemsPerPage;
    vm.transition = transition;
    vm.criteria = criteria;
    vm.del = del;

    vm.loadAll();

    function loadAll() {
        AuthorityService.query({
            page: pagingParams.page - 1,
            size: vm.itemsPerPage,
            sort: sort()
        }, function (result, headers) {
            vm.links = ParseLinksUtils.parse(headers('link'));
            vm.totalItems = headers('X-Total-Count');
            vm.page = pagingParams.page;
            vm.entities = result;
        });
    }

    function sort() {
        var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
        if (vm.predicate !== 'name') {
            // default sort column
            result.push('name,asc');
        }
        return result;
    }

    function loadPage(page) {
        vm.page = page;
        vm.transition();
    }

    function transition() {
        $state.transitionTo($state.$current, {
            page: vm.page,
            sort: vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')
        });
    }

    function checkPressEnter($event) {
        //按下enter键重新查询数据
        if ($event.keyCode == 13) {
            vm.transition();
        }
    }

    function setEnabled(entity, enabled) {
        entity.enabled = enabled;
        AuthorityService.update(entity,
            function () {
                vm.loadAll();
            },
            function () {
                entity.enabled = !enabled;
            });
    }

    function del(name) {
        AlertUtils.createDeleteConfirmation('The data may be referenced by other data, and there may be some problems after deletion, are you sure to delete?', function (isConfirm) {
            if (isConfirm) {
                AuthorityService.del({name: name},
                    function () {
                        vm.loadAll();
                    },
                    function () {
                    });
            }
        });
    }
}

/**
 * AuthorityDialogController
 */
function AuthorityDialogController($state, $stateParams, $uibModalInstance, AuthorityService, entity) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.mode = $state.current.data.mode;
    vm.entity = entity;
    vm.isSaving = false;
    vm.save = save;
    vm.cancel = cancel;

    function save() {
        vm.isSaving = true;
        if (vm.mode == 'edit') {
            AuthorityService.update(vm.entity, onSaveSuccess, onSaveError);
        } else {
            AuthorityService.create(vm.entity, onSaveSuccess, onSaveError);
        }
    }

    function onSaveSuccess(result) {
        vm.isSaving = false;
        $uibModalInstance.close(result);
    }

    function onSaveError(result) {
        vm.isSaving = false;
    }

    function cancel() {
        $uibModalInstance.dismiss('cancel');
    }
}

/**
 * UserListController
 */
function UserListController($state, AlertUtils, ParseLinksUtils, PAGINATION_CONSTANTS, pagingParams, criteria, UserService, PrincipalService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.parentPageTitle = $state.$current.parent.data.pageTitle;
    vm.currentAccount = null;
    vm.links = null;
    vm.loadAll = loadAll;
    vm.loadPage = loadPage;
    vm.checkPressEnter = checkPressEnter;
    vm.page = 1;
    vm.setActive = setActive;
    vm.setEnabled = setEnabled;
    vm.totalItems = null;
    vm.entities = [];
    vm.predicate = pagingParams.predicate;
    vm.reverse = pagingParams.ascending;
    vm.itemsPerPage = PAGINATION_CONSTANTS.itemsPerPage;
    vm.transition = transition;
    vm.criteria = criteria;
    vm.del = del;
    vm.resetPassword = resetPassword;

    vm.loadAll();

    PrincipalService.identity().then(function (account) {
        vm.currentAccount = account;
    });

    function loadAll() {
        UserService.query({
            page: pagingParams.page - 1,
            size: vm.itemsPerPage,
            sort: sort(),
            login: vm.criteria.login
        }, function (result, headers) {
            //hide anonymous user from user management: it's a required user for Spring Security
            for (var i in result) {
                if (result[i]['userName'] === 'anonymoususer') {
                    result.splice(i, co1);
                }
            }
            vm.links = ParseLinksUtils.parse(headers('link'));
            vm.totalItems = headers('X-Total-Count');
            vm.page = pagingParams.page;
            vm.entities = result;
        });
    }

    function sort() {
        var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
        if (vm.predicate !== 'modifiedTime') {
            // default sort column
            result.push('modifiedTime,desc');
        }
        return result;
    }

    function loadPage(page) {
        vm.page = page;
        vm.transition();
    }

    function transition() {
        $state.transitionTo($state.$current, {
            page: vm.page,
            sort: vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc'),
            login: vm.criteria.login
        });
    }

    function checkPressEnter($event) {
        //按下enter键重新查询数据
        if ($event.keyCode == 13) {
            vm.transition();
        }
    }

    function setActive(user, isActivated) {
        user.activated = isActivated;
        UserService.update(user, function () {
                vm.loadAll();
            },
            function () {
                user.activated = !isActivated;
            });
    }

    function setEnabled(entity, enabled) {
        entity.enabled = enabled;
        UserService.update(entity,
            function () {
                vm.loadAll();
            },
            function () {
                entity.enabled = !enabled;
            });
    }

    function del(userName) {
        AlertUtils.createDeleteConfirmation('The data may be referenced by other data, and there may be some problems after deletion, are you sure to delete?', function (isConfirm) {
            if (isConfirm) {
                UserService.del({userName: userName},
                    function () {
                        vm.loadAll();
                    },
                    function () {
                    });
            }
        });
    }

    function resetPassword(userName) {
        AlertUtils.createResetPasswordConfirmation('Reset password?', function (isConfirm) {
            if (isConfirm) {
                UserService.resetPassword({userName: userName},
                    function () {
                    });
            }
        });
    }
}

/**
 * UserDialogController
 */
function UserDialogController($state, $stateParams, $uibModalInstance, UserService, AccountService, entity) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.mode = $state.current.data.mode;
    vm.authorities = AccountService.queryAuthorityNames({enabled: true});
    vm.entity = entity;
    vm.isSaving = false;
    vm.save = save;
    vm.cancel = cancel;

    if (vm.mode == 'create') {
        vm.entity.authorities = ["ROLE_USER"];
    }

    function save() {
        vm.isSaving = true;
        if (vm.mode == 'edit') {
            UserService.update(vm.entity, onSaveSuccess, onSaveError);
        } else {
            UserService.create(vm.entity, onSaveSuccess, onSaveError);
        }
    }

    function onSaveSuccess(result) {
        vm.isSaving = false;
        $uibModalInstance.close(result);
    }

    function onSaveError(result) {
        vm.isSaving = false;
    }

    function cancel() {
        $uibModalInstance.dismiss('cancel');
    }
}

/**
 * UserDetailsController
 */
function UserDetailsController($state, $stateParams, entity, AuthServerService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.parentPageTitle = $state.$current.parent.data.pageTitle;
    vm.grandfatherPageTitle = $state.$current.parent.parent.data.pageTitle;
    vm.entity = entity;

    var authToken = AuthServerService.getToken();
    if (authToken) {
        vm.entity.profilePhotoUrl = '/api/users/profile-photo/' + vm.entity.userName + '?access_token=' + authToken.access_token;
    }
}

/**
 * AppListController
 */
function AppListController($state, AlertUtils, ParseLinksUtils, PAGINATION_CONSTANTS, pagingParams, criteria, AppService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.parentPageTitle = $state.$current.parent.data.pageTitle;
    vm.links = null;
    vm.loadAll = loadAll;
    vm.loadPage = loadPage;
    vm.checkPressEnter = checkPressEnter;
    vm.page = 1;
    vm.setEnabled = setEnabled;
    vm.totalItems = null;
    vm.entities = [];
    vm.predicate = pagingParams.predicate;
    vm.reverse = pagingParams.ascending;
    vm.itemsPerPage = PAGINATION_CONSTANTS.itemsPerPage;
    vm.transition = transition;
    vm.criteria = criteria;
    vm.del = del;

    vm.loadAll();

    function loadAll() {
        AppService.query({
            page: pagingParams.page - 1,
            size: vm.itemsPerPage,
            sort: sort()
        }, function (result, headers) {
            vm.links = ParseLinksUtils.parse(headers('link'));
            vm.totalItems = headers('X-Total-Count');
            vm.page = pagingParams.page;
            vm.entities = result;
        });
    }

    function sort() {
        var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
        if (vm.predicate !== 'name') {
            // default sort column
            result.push('name,asc');
        }
        return result;
    }

    function loadPage(page) {
        vm.page = page;
        vm.transition();
    }

    function transition() {
        $state.transitionTo($state.$current, {
            page: vm.page,
            sort: vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')
        });
    }

    function checkPressEnter($event) {
        //按下enter键重新查询数据
        if ($event.keyCode == 13) {
            vm.transition();
        }
    }

    function setEnabled(entity, enabled) {
        entity.enabled = enabled;
        AppService.update(entity,
            function () {
                vm.loadAll();
            },
            function () {
                entity.enabled = !enabled;
            });
    }

    function del(name) {
        AlertUtils.createDeleteConfirmation('The data may be referenced by other data, and there may be some problems after deletion, are you sure to delete?', function (isConfirm) {
            if (isConfirm) {
                AppService.del({name: name},
                    function () {
                        vm.loadAll();
                    },
                    function () {
                    });
            }
        });
    }
}

/**
 * AppDialogController
 */
function AppDialogController($state, $stateParams, $uibModalInstance, AppService, AccountService, entity) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.mode = $state.current.data.mode;
    vm.authorities = AccountService.queryAuthorityNames({enabled: true});
    vm.entity = entity;
    vm.isSaving = false;
    vm.save = save;
    vm.cancel = cancel;

    function save() {
        vm.isSaving = true;
        if (vm.mode == 'edit') {
            AppService.update(vm.entity, onSaveSuccess, onSaveError);
        } else {
            AppService.create(vm.entity, onSaveSuccess, onSaveError);
        }
    }

    function onSaveSuccess(result) {
        vm.isSaving = false;
        $uibModalInstance.close(result);
    }

    function onSaveError(result) {
        vm.isSaving = false;
    }

    function cancel() {
        $uibModalInstance.dismiss('cancel');
    }
}

/**
 * AppDetailsController
 */
function AppDetailsController($state, $stateParams, entity) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.parentPageTitle = $state.$current.parent.data.pageTitle;
    vm.grandfatherPageTitle = $state.$current.parent.parent.data.pageTitle;
    vm.entity = entity;
}

/**
 * AdminMenuListController
 */
function AdminMenuListController($state, AlertUtils, ParseLinksUtils, PAGINATION_CONSTANTS, pagingParams, criteria, AdminMenuService, AppService, APP_NAME) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.parentPageTitle = $state.$current.parent.data.pageTitle;
    vm.apps = AppService.query();
    vm.links = null;
    vm.loadAll = loadAll;
    vm.loadPage = loadPage;
    vm.checkPressEnter = checkPressEnter;
    vm.page = 1;
    vm.totalItems = null;
    vm.entities = [];
    vm.predicate = pagingParams.predicate;
    vm.reverse = pagingParams.ascending;
    vm.itemsPerPage = PAGINATION_CONSTANTS.itemsPerPage;
    vm.transition = transition;
    vm.criteria = criteria;
    vm.del = del;
    vm.moveUp = moveUp;
    vm.moveDown = moveDown;

    vm.loadAll();

    function loadAll() {
        AdminMenuService.query({
            page: pagingParams.page - 1,
            size: vm.itemsPerPage,
            sort: sort(),
            appName: vm.criteria.app
        }, function (result, headers) {
            vm.links = ParseLinksUtils.parse(headers('link'));
            vm.totalItems = headers('X-Total-Count');
            vm.page = pagingParams.page;
            vm.entities = result;
        });
    }

    function sort() {
        var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
        if (vm.predicate !== 'modifiedTime') {
            // default sort column
            result.push('modifiedTime,desc');
        }
        return result;
    }

    function loadPage(page) {
        vm.page = page;
        vm.transition();
    }

    function transition() {
        $state.transitionTo($state.$current, {
            page: vm.page,
            sort: vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc'),
            app: vm.criteria.app
        });
    }

    function checkPressEnter($event) {
        //按下enter键重新查询数据
        if ($event.keyCode == 13) {
            vm.transition();
        }
    }

    function del(id) {
        AlertUtils.createDeleteConfirmation('The data may be referenced by other data, and there may be some problems after deletion, are you sure to delete?', function (isConfirm) {
            if (isConfirm) {
                AdminMenuService.del({id: id},
                    function () {
                        vm.loadAll();
                    },
                    function () {
                    });
            }
        });
    }

    function moveUp(id) {
        AdminMenuService.moveUp({id: id},
            function () {
                vm.loadAll()
            });
    }

    function moveDown(id) {
        AdminMenuService.moveDown({id: id},
            function () {
                vm.loadAll()
            });
    }
}

/**
 * AdminMenuDialogController
 */
function AdminMenuDialogController($state, $stateParams, $uibModalInstance, AdminMenuService, AppService, APP_NAME, entity) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.mode = $state.current.data.mode;
    vm.apps = AppService.query();
    vm.searchParentMenus = searchParentMenus;
    vm.entity = entity;
    vm.isSaving = false;
    vm.save = save;
    vm.cancel = cancel;

    vm.searchParentMenus();

    function save() {
        vm.isSaving = true;
        vm.entity.level = 1;
        if (vm.entity.parentId) {
            vm.entity.level = 2;
        }
        if (vm.mode == 'edit') {
            AdminMenuService.update(vm.entity, onSaveSuccess, onSaveError);
        } else {
            AdminMenuService.create(vm.entity, onSaveSuccess, onSaveError);
        }
    }

    function searchParentMenus() {
        if (vm.entity && vm.entity.appName) {
            vm.parentMenus = AdminMenuService.queryParents({appName: vm.entity.appName});
        }
        else {
            vm.parentMenus = [];
        }
    }

    function onSaveSuccess(result) {
        vm.isSaving = false;
        $uibModalInstance.close(result);
    }

    function onSaveError(result) {
        vm.isSaving = false;
    }

    function cancel() {
        $uibModalInstance.dismiss('cancel');
    }
}

/**
 * AuthorityAdminMenuController
 */
function AuthorityAdminMenuController($state, AuthorityAdminMenuService, AppAuthorityService, AppService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.parentPageTitle = $state.$current.parent.data.pageTitle;
    vm.apps = AppService.query();
    vm.authorities = [];
    vm.allMenus = [];
    vm.isSaving = false;
    vm.searchAuthorities = searchAuthorities;
    vm.searchMenus = searchMenus;
    vm.save = save;

    function searchAuthorities() {
        if (vm.criteria && vm.criteria.appName) {
            vm.authorities = AppAuthorityService.query({appName: vm.criteria.appName});
        }
        else {
            vm.authorities = [];
        }
    }

    function searchMenus() {
        vm.allMenus = [];
        if (vm.criteria.authorityName) {
            AuthorityAdminMenuService.query({
                appName: vm.criteria.appName,
                authorityName: vm.criteria.authorityName
            }, function (response) {
                vm.allMenus = response;
            });
        }
    }

    function save() {
        vm.isSaving = true;
        if (vm.criteria.appName && vm.criteria.authorityName) {
            var adminMenuIds = getAllCheckIds(vm.allMenus, []);
            AuthorityAdminMenuService.update({
                    appName: vm.criteria.appName,
                    authorityName: vm.criteria.authorityName,
                    adminMenuIds: adminMenuIds
                },
                function (response) {
                    vm.isSaving = false;
                }, function (errorResponse) {
                    vm.isSaving = false;
                });
        }
    }

    function getAllCheckIds(allMenus, adminMenuIds) {
        for (var i = 0; i < allMenus.length; i++) {
            if (allMenus[i].checked) {
                adminMenuIds.push(allMenus[i].id);
            }
            if (allMenus[i].children) {
                getAllCheckIds(allMenus[i].children, adminMenuIds);
            }
        }
        return adminMenuIds;
    }
}

/**
 * OAuth2ClientListController
 */
function OAuth2ClientListController($state, AlertUtils, ParseLinksUtils, PAGINATION_CONSTANTS, pagingParams, criteria, OAuth2ClientService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.parentPageTitle = $state.$current.parent.data.pageTitle;
    vm.links = null;
    vm.loadAll = loadAll;
    vm.loadPage = loadPage;
    vm.checkPressEnter = checkPressEnter;
    vm.page = 1;
    vm.totalItems = null;
    vm.entities = [];
    vm.predicate = pagingParams.predicate;
    vm.reverse = pagingParams.ascending;
    vm.itemsPerPage = PAGINATION_CONSTANTS.itemsPerPage;
    vm.transition = transition;
    vm.criteria = criteria;
    vm.del = del;

    vm.loadAll();

    function loadAll() {
        OAuth2ClientService.query({
            page: pagingParams.page - 1,
            size: vm.itemsPerPage,
            sort: sort(),
            clientId: vm.criteria.clientId
        }, function (result, headers) {
            vm.links = ParseLinksUtils.parse(headers('link'));
            vm.totalItems = headers('X-Total-Count');
            vm.page = pagingParams.page;
            vm.entities = result;
        });
    }

    function sort() {
        var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
        if (vm.predicate !== 'client_id') {
            // default sort column
            result.push('client_id,asc');
        }
        return result;
    }

    function loadPage(page) {
        vm.page = page;
        vm.transition();
    }

    function transition() {
        $state.transitionTo($state.$current, {
            page: vm.page,
            sort: vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc'),
            clientId: vm.criteria.clientId
        });
    }

    function checkPressEnter($event) {
        //按下enter键重新查询数据
        if ($event.keyCode == 13) {
            vm.transition();
        }
    }

    function del(id) {
        AlertUtils.createDeleteConfirmation('The data may be referenced by other data, and there may be some problems after deletion, are you sure to delete?', function (isConfirm) {
            if (isConfirm) {
                OAuth2ClientService.del({id: id},
                    function () {
                        vm.loadAll();
                    },
                    function () {
                    });
            }
        });
    }
}

/**
 * OAuth2ClientDialogController
 */
function OAuth2ClientDialogController($scope, $state, $stateParams, $uibModalInstance, OAuth2ClientService, entity) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.mode = $state.current.data.mode;
    vm.entity = entity;
    vm.isSaving = false;
    vm.save = save;
    vm.cancel = cancel;
    vm.addUri = addUri;
    vm.delUri = delUri;

    if (vm.mode == 'create') {
        vm.entity.redirect_uri.push("");
    }

    $scope.$watch('vm.entity.scope', function (newValue) {
        if (newValue) {
            if (_.isArray(newValue)) {
                vm.entity.scopeArray = newValue;
            } else {
                vm.entity.scopeArray = newValue.split(',');
            }
        }
        else {
            vm.entity.scopeArray = [];
        }
    });
    vm.entity.scopeArray = vm.entity.scope;


    function save() {
        vm.isSaving = true;

        vm.entity.autoapprove = _.intersection(vm.entity.autoapprove, vm.entity.scopeArray);

        if (vm.mode == 'edit') {
            OAuth2ClientService.update(vm.entity, onSaveSuccess, onSaveError);
        } else {
            OAuth2ClientService.create(vm.entity, onSaveSuccess, onSaveError);
        }
    }

    function onSaveSuccess(result) {
        vm.isSaving = false;
        $uibModalInstance.close(result);
    }

    function onSaveError(result) {
        vm.isSaving = false;
    }

    function cancel() {
        $uibModalInstance.dismiss('cancel');
    }

    function addUri() {
        vm.entity.redirect_uri.length++;
    }

    function delUri(index) {
        // Remove element
        vm.entity.redirect_uri.splice(index, 1);
    }
}

/**
 * OAuth2ClientDetailsController
 */
function OAuth2ClientDetailsController($state, $stateParams, entity) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.parentPageTitle = $state.$current.parent.data.pageTitle;
    vm.grandfatherPageTitle = $state.$current.parent.parent.data.pageTitle;
    vm.entity = entity;
}

/**
 * OAuth2AccessTokenListController
 */
function OAuth2AccessTokenListController($state, AlertUtils, ParseLinksUtils, PAGINATION_CONSTANTS, pagingParams, criteria, OAuth2AccessTokenService, AuthServerService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.parentPageTitle = $state.$current.parent.data.pageTitle;
    vm.currentAccessToken = AuthServerService.getAccessToken();
    vm.links = null;
    vm.loadAll = loadAll;
    vm.loadPage = loadPage;
    vm.checkPressEnter = checkPressEnter;
    vm.page = 1;
    vm.totalItems = null;
    vm.entities = [];
    vm.predicate = pagingParams.predicate;
    vm.reverse = pagingParams.ascending;
    vm.itemsPerPage = PAGINATION_CONSTANTS.itemsPerPage;
    vm.transition = transition;
    vm.criteria = criteria;
    vm.del = del;
    vm.goRefreshToken = goRefreshToken;

    vm.loadAll();

    function loadAll() {
        OAuth2AccessTokenService.query({
            page: pagingParams.page - 1,
            size: vm.itemsPerPage,
            sort: sort(),
            tokenId: vm.criteria.tokenId,
            clientId: vm.criteria.clientId,
            userName: vm.criteria.userName,
            refreshToken: vm.criteria.refreshToken
        }, function (result, headers) {
            vm.links = ParseLinksUtils.parse(headers('link'));
            vm.totalItems = headers('X-Total-Count');
            vm.page = pagingParams.page;
            vm.entities = result;
        });
    }

    function sort() {
        var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
        if (vm.predicate !== 'modifiedTime') {
            // default sort column
            result.push('modifiedTime,desc');
        }
        return result;
    }

    function loadPage(page) {
        vm.page = page;
        vm.transition();
    }

    function transition() {
        $state.transitionTo($state.$current, {
            page: vm.page,
            sort: vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc'),
            tokenId: vm.criteria.tokenId,
            clientId: vm.criteria.clientId,
            userName: vm.criteria.userName,
            refreshToken: vm.criteria.refreshToken
        });
    }

    function checkPressEnter($event) {
        //按下enter键重新查询数据
        if ($event.keyCode == 13) {
            vm.transition();
        }
    }

    function del(id) {
        AlertUtils.createDeleteConfirmation('删除会使该用户退出系统，您确定删除吗?', function (isConfirm) {
            if (isConfirm) {
                OAuth2AccessTokenService.del({id: id},
                    function () {
                        vm.loadAll();
                    },
                    function () {
                    });
            }
        });
    }

    function goRefreshToken(refreshToken) {
        $state.go('security.oauth2-refresh-token-list', {'tokenId': refreshToken});
    }
}

/**
 * OAuth2AccessTokenDetailsController
 */
function OAuth2AccessTokenDetailsController($state, $stateParams, entity) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.parentPageTitle = $state.$current.parent.data.pageTitle;
    vm.grandfatherPageTitle = $state.$current.parent.parent.data.pageTitle;
    vm.entity = entity;
}

/**
 * OAuth2RefreshTokenListController
 */
function OAuth2RefreshTokenListController($state, AlertUtils, ParseLinksUtils, PAGINATION_CONSTANTS, pagingParams, criteria, OAuth2RefreshTokenService, AuthServerService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.parentPageTitle = $state.$current.parent.data.pageTitle;
    vm.currentRefreshToken = AuthServerService.getRefreshToken();
    vm.links = null;
    vm.loadAll = loadAll;
    vm.loadPage = loadPage;
    vm.checkPressEnter = checkPressEnter;
    vm.page = 1;
    vm.totalItems = null;
    vm.entities = [];
    vm.predicate = pagingParams.predicate;
    vm.reverse = pagingParams.ascending;
    vm.itemsPerPage = PAGINATION_CONSTANTS.itemsPerPage;
    vm.transition = transition;
    vm.criteria = criteria;
    vm.del = del;

    vm.loadAll();

    function loadAll() {
        OAuth2RefreshTokenService.query({
            page: pagingParams.page - 1,
            size: vm.itemsPerPage,
            sort: sort(),
            tokenId: vm.criteria.tokenId,
            clientId: vm.criteria.clientId,
            userName: vm.criteria.userName
        }, function (result, headers) {
            vm.links = ParseLinksUtils.parse(headers('link'));
            vm.totalItems = headers('X-Total-Count');
            vm.page = pagingParams.page;
            vm.entities = result;
        });
    }

    function sort() {
        var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
        if (vm.predicate !== 'modifiedTime') {
            // default sort column
            result.push('modifiedTime,desc');
        }
        return result;
    }

    function loadPage(page) {
        vm.page = page;
        vm.transition();
    }

    function transition() {
        $state.transitionTo($state.$current, {
            page: vm.page,
            sort: vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc'),
            tokenId: vm.criteria.tokenId,
            clientId: vm.criteria.clientId,
            userName: vm.criteria.userName
        });
    }

    function checkPressEnter($event) {
        //按下enter键重新查询数据
        if ($event.keyCode == 13) {
            vm.transition();
        }
    }

    function del(id) {
        AlertUtils.createDeleteConfirmation('删除会使该用户退出系统，您确定删除吗?', function (isConfirm) {
            if (isConfirm) {
                OAuth2RefreshTokenService.del({id: id},
                    function () {
                        vm.loadAll();
                    },
                    function () {
                    });
            }
        });
    }
}

/**
 * OAuth2RefreshTokenDetailsController
 */
function OAuth2RefreshTokenDetailsController($state, $stateParams, entity) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.parentPageTitle = $state.$current.parent.data.pageTitle;
    vm.grandfatherPageTitle = $state.$current.parent.parent.data.pageTitle;
    vm.entity = entity;
}

/**
 * OAuth2ApprovalListController
 */
function OAuth2ApprovalListController($state, AlertUtils, ParseLinksUtils, PAGINATION_CONSTANTS, pagingParams, criteria, OAuth2ApprovalService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.parentPageTitle = $state.$current.parent.data.pageTitle;
    vm.links = null;
    vm.loadAll = loadAll;
    vm.loadPage = loadPage;
    vm.checkPressEnter = checkPressEnter;
    vm.page = 1;
    vm.totalItems = null;
    vm.entities = [];
    vm.predicate = pagingParams.predicate;
    vm.reverse = pagingParams.ascending;
    vm.itemsPerPage = PAGINATION_CONSTANTS.itemsPerPage;
    vm.transition = transition;
    vm.criteria = criteria;
    vm.del = del;

    vm.loadAll();

    function loadAll() {
        OAuth2ApprovalService.query({
            page: pagingParams.page - 1,
            size: vm.itemsPerPage,
            sort: sort(),
            approvalId: vm.criteria.approvalId,
            clientId: vm.criteria.clientId,
            userName: vm.criteria.userName
        }, function (result, headers) {
            vm.links = ParseLinksUtils.parse(headers('link'));
            vm.totalItems = headers('X-Total-Count');
            vm.page = pagingParams.page;
            vm.entities = result;
        });
    }

    function sort() {
        var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
        if (vm.predicate !== 'clientId') {
            // default sort column
            result.push('clientId,asc');
        }
        return result;
    }

    function loadPage(page) {
        vm.page = page;
        vm.transition();
    }

    function transition() {
        $state.transitionTo($state.$current, {
            page: vm.page,
            sort: vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc'),
            approvalId: vm.criteria.approvalId,
            clientId: vm.criteria.clientId,
            userName: vm.criteria.userName
        });
    }

    function checkPressEnter($event) {
        //按下enter键重新查询数据
        if ($event.keyCode == 13) {
            vm.transition();
        }
    }

    function del(id) {
        AlertUtils.createDeleteConfirmation('删除会使该用户退出系统，您确定删除吗?', function (isConfirm) {
            if (isConfirm) {
                OAuth2ApprovalService.del({id: id},
                    function () {
                        vm.loadAll();
                    },
                    function () {
                    });
            }
        });
    }
}

/**
 * OAuth2ApprovalDetailsController
 */
function OAuth2ApprovalDetailsController($state, $stateParams, entity, $scope, $rootScope) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.parentPageTitle = $state.$current.parent.data.pageTitle;
    vm.grandfatherPageTitle = $state.$current.parent.parent.data.pageTitle;
    vm.entity = entity;
}
