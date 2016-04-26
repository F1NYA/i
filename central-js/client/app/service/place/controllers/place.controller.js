angular.module('app').controller('PlaceController',
  function ($q,
            $state,
            AdminService,
            $rootScope,
            $scope,
            $location,
            $sce,
            $modal,
            RegionListFactory,
            LocalityListFactory,
            PlacesService,
            ServiceService,
            UserService,
            regions,
            service) {

    var self = this;
    var oService = ServiceService.oService;

    //$scope.oService = oService;
    $scope.service = service;
    $scope.regions = regions;
    $scope.$state = $state;
    $scope.$location = $location;
    $scope.bAdmin = AdminService.isAdmin();
    $scope.state = $state.get($state.current.name);

    function getBuiltInBankIDStateParams() {
      return {
        'id': service.nID,
        'region': $scope.getRegionId(),
        'city': $scope.getCityId()
      }
    }

    function isPlaceChoosingState(state) {
      return state.current.name === 'index.service.general.place.built-in'
    }

    function isLoggedIn() {
      return UserService.isLoggedIn().then(function () {
        return {isStaying: false, isLoggedIn: true};
      }).catch(function () {
        return {isStaying: true, isLoggedIn: false};
      });
    }

    function isStayingOnCurrentState(state, stateToGo) {
      var isStaying = !stateToGo
        || ($state.current.name === stateToGo)
        || $state.current.name === 'index.service.general.place.built-in.bankid'
        || $state.current.name === 'index.service.general.place.built-in.bankid.submitted'
      return {isStaying: isStaying, isLoggedIn: false};
    }

    /**
     * Обробити зміну місця
     */
    $scope.$on('onPlaceChange', function (evt) {
      // діємо в залежності від доступності сервісу
      var stateToGo = PlacesService.getServiceStateForPlace();

      // отримати дані сервісу та його опис
      var oAvail = PlacesService.serviceAvailableIn();
      var foundInCountry;
      var foundInRegion;
      var foundInCity;

      angular.forEach(oService.aServiceData, function (oServiceData, key) {
        foundInCountry = oAvail.thisCountry;
        foundInRegion = oAvail.thisRegion && oServiceData.nID_Region && oServiceData.nID_Region.nID === $scope.getRegionId();
        foundInCity = oAvail.thisCity && oServiceData.nID_City && oServiceData.nID_City.nID === $scope.getCityId();
        // if (service.nID_Region && service.nID_Region.nID === $scope.getRegionId() && service.nID_City && service.nID_City.nID === $scope.getCityId()) {
        $scope.oService = oService;
        if (foundInCountry || foundInRegion || foundInCity) {
          $scope.serviceData = oServiceData;
          if (oService.bNoteTrusted === false) {
            $scope.serviceData.sNote = $sce.trustAsHtml($scope.serviceData.sNote);
            oService.sNoteTrusted = true;
          }
        }
      });

      // console.info('PROCESS Place сhange, $state:', $state.current.name, ', to go:', stateToGo);

      $q.when(isPlaceChoosingState($state) ? isLoggedIn() : isStayingOnCurrentState($state, stateToGo))
        .then(function (result) {
          if (result.isLoggedIn) {
            $state.go('index.service.general.place.built-in.bankid', getBuiltInBankIDStateParams(), {reload : true});
          } else if (!result.isLoggedIn && !result.isStaying) {
            $state.go(stateToGo, {id: oService.nID}, {location: false})
          }
        });
    });

    /**
     * Перейти до стану редагування місця
     */
    $scope.$on('onPlaceEdit', function (evt) {
      // TODO ще раз перевірити, як це працює у різних контекстах
      // можливо, треба переходити на попередній стан, а не на фіксований
      return $state.go('index.service.general.place', {
        id: oService.nID
      }, {
        location: false
      }).then(function () {
        //
      });
    });

    $scope.getRegionId = function () {
      var place = PlacesService.getPlaceData();
      var region = place ? place.region || null : null;
      return region ? region.nID : 0;
    };

    $scope.getCityId = function () {
      var place = PlacesService.getPlaceData();
      var city = place ? place.city || null : null;
      return city ? city.nID : 0;
    };

    $scope.getAuthMethods = function () {
      var authMethods;

      if ($scope.serviceData) {
        if ($scope.serviceData.asAuth && $scope.serviceData.asAuth.length > 0) {
          if (typeof($scope.serviceData.asAuth) === 'string') {
            authMethods = $scope.serviceData.asAuth;
          } else {
            //TODO processing of object O_o
            authMethods = $scope.serviceData.asAuth;
          }
        }
      }

      return authMethods;
    };

    $scope.getRedirectUrl = function () {
      var stateForRedirect = $state.href('index.service.general.place.built-in.bankid', getBuiltInBankIDStateParams());
      return $location.protocol() +
        '://' + $location.host() + ':'
        + $location.port()
        + stateForRedirect;
    }

  });
