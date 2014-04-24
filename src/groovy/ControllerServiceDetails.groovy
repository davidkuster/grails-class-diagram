
class ControllerServiceDetails extends ClassDetails {


    ControllerServiceDetails(grailsApplication, ClassDiagramPreferences prefs) {
        super(prefs)

        def allControllers = grailsApplication.controllerClasses
        def allServices = grailsApplication.serviceClasses
        def allControllersAndServices = allControllers + allServices

        classesToExclude = excludeSelection(allControllersAndServices)
        classesToDiagram = includeSelection(allControllersAndServices) - classesToExclude
    }

}