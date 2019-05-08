


def pre_export(output, conanfile, conanfile_path, reference, **kwargs):
    ref = str(reference)
    if ref.lower() != ref:
        raise Exception("Reference %s should be all lowercase" % ref)
    if "-" in ref:
        raise Exception("Use _ instead of -")

