
abstract class ClassDetails {

    def grailsApp
    ClassDiagramPreferences prefs

    List<ClassDetail> classesToDiagram
    List<ClassDetail> classesToExclude


    ClassDetails(grailsApplication, ClassDiagramPreferences prefs) {
        this.grailsApp = grailsApplication
        this.prefs = prefs
    }


    protected includeSelection(allDomains) {
        ClassDiagramUtil.randomizeOrder(
            ClassDiagramUtil.classSelection(allDomains, prefs),
            prefs )
    }

    protected excludeSelection(allDomains) {
        ClassDiagramUtil.excludeSelection(allDomains, prefs)
    }


    Set getAllPackageNames() {
        getPackageNames( classesToDiagram )
    }

    protected Collection getPackageNames(classes) {
        ClassDiagramUtil.randomizeOrder(
            classes?.collect { ClassDiagramUtil.getPackageName(it) } as Set,
            prefs )
    }


    Set getClassesToDiagramForPackage(String packageName) {
        // TODO: this should be able to be cleaned up further
        // need to figure out why getPackageName() is doing what it's doing
        classesToDiagram.findAll { ClassDiagramUtil.getPackageName(it) == packageName }
    }

}