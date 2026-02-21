#!/bin/bash
tar -czvf SecureSrc-1.0.0.tar.gz -T releaseFiles.txt --transform 's|^|SecureSrc-1.0.0/|'