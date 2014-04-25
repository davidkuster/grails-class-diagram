
class ControllerServiceDetails extends ClassDetails {


    ControllerServiceDetails(grailsApplication, ClassDiagramPreferences prefs) {
        super(grailsApplication, prefs)

        def allControllers = grailsApplication.controllerClasses as List
        def allServices = grailsApplication.serviceClasses as List
        def allControllersAndServices = allControllers + allServices

        classesToExclude = excludeSelection(allControllersAndServices)
        classesToDiagram = includeSelection(allControllersAndServices) - classesToExclude
    }

}