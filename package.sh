#!/bin/bash
tar -czvf SecureSrc-1.0.1.tar.gz -T releaseFiles.txt --transform 's|^|SecureSrc-1.0.1/|'