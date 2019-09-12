


def pre_export(output, conanfile, conanfile_path, reference, **kwargs):
    ref = str(reference)
    if ref.lower() != ref:
        raise Exception("Reference %s should be all lowercase" % ref)
    # use python "in" syntax.... if "something" in ref:
    #TODO
