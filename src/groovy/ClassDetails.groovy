
abstract class ClassDetails {

    ClassDiagramPreferences prefs

    def classesToDiagram
    def classesToExclude

    // TO deal with the issue to know the Grails 's injected methods,
    // we list here the commonsMethod between Grails domain class, supposed to be injected.
    // commonsMethods is populated in buildDomainClasses()
    List commonsMethods = new ArrayList()


    ClassDetails(ClassDiagramPreferences prefs) {
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

    protected getPackageNames(classes) {
        ClassDiagramUtil.randomizeOrder(
            classes?.collect { ClassDiagramUtil.getPackageName(it) } as Set,
            prefs )
    }



    //abstract getInterestingProperties(cls)

    //abstract getInterestingAssociations(cls)

    //abstract getInterestingMethods(cls)


    /**
     * Get methods declared in a class, filtering out all inherited and meta-added stuff.
     * Quite a few assumptions are made, no satisfactory solution found. Hack!
     * The Class.getDeclaredMethods() also includes decorated methods, which makes it essentially useless.
     * I think we need a grails getUndecoratedDeclaredMethods() that gives us what we really coded in the class, if that is possible.
     */
    protected getDeclaredMethods(methods, propertyNames) {
        def filterMethods = methods.findAll { it.name =~ /\$/} // remove special methods containing $
        filterMethods += GroovyObject.methods.flatten() // remove metaClass, properties etc.
        filterMethods += Object.methods.flatten() // remove toString

        commonsMethods.each { methodtoremove ->
            filterMethods += methods.findAll {it.name =~ methodtoremove}
        }

        // filter out property-related methods
        methods.each { method ->
            ["get","is","set","addTo","removeFrom"].each { prefix ->
                propertyNames.each{ propertyName ->
                    if (method.name == prefix + ClassDiagramUtil.initCap(propertyName)) {
                        // TODO: Filter by signature, not just name
                        filterMethods += method
                    }
                }
            }
        }

        // Apply filter
        filterMethods.each {filterMethod ->
            methods -= methods.find { ClassDiagramUtil.hasSameSignature(it, filterMethod)}
        }

        return methods
    }

}