# -*- mode: python ; coding: utf-8 -*-
from PyInstaller.utils.hooks import collect_all, collect_submodules

# Collect all data files, binaries, and hidden imports for MLX packages.
# MLX has native .dylib, .metallib, and .so files that PyInstaller won't auto-detect.
mlx_datas, mlx_binaries, mlx_hiddenimports = collect_all('mlx')
mlx_whisper_datas, mlx_whisper_binaries, mlx_whisper_hiddenimports = collect_all('mlx_whisper')
mlx_lm_datas, mlx_lm_binaries, mlx_lm_hiddenimports = collect_all('mlx_lm')

# Also collect faster-whisper, llama-cpp, huggingface, and imageio-ffmpeg dependencies
# imageio_ffmpeg bundles its own ffmpeg binary as package data that must be collected
fw_datas, fw_binaries, fw_hiddenimports = collect_all('faster_whisper')
hf_datas, hf_binaries, hf_hiddenimports = collect_all('huggingface_hub')
ff_datas, ff_binaries, ff_hiddenimports = collect_all('imageio_ffmpeg')

import mlx
import os
mlx_metallib_path = os.path.join(mlx.__path__[0], 'lib', 'mlx.metallib')
extra_datas = [(mlx_metallib_path, '.')] if os.path.exists(mlx_metallib_path) else []

all_datas = mlx_datas + mlx_whisper_datas + mlx_lm_datas + fw_datas + hf_datas + ff_datas + extra_datas
all_binaries = mlx_binaries + mlx_whisper_binaries + mlx_lm_binaries + fw_binaries + hf_binaries + ff_binaries
all_hiddenimports = (
    mlx_hiddenimports + mlx_whisper_hiddenimports + mlx_lm_hiddenimports +
    fw_hiddenimports + hf_hiddenimports + ff_hiddenimports +
    collect_submodules('mlx') +
    collect_submodules('mlx_lm') +
    collect_submodules('mlx_lm.models') +
    collect_submodules('mlx_whisper') +
    [
        'imageio_ffmpeg',
        'imageio_ffmpeg._utils',
        'requests',
        'urllib3',
        'certifi',
        'charset_normalizer',
        'idna',
        'tqdm',
        'regex',
        'safetensors',
        'tokenizers',
        'sentencepiece',
        'numpy',
    ]
)

a = Analysis(
    ['bridge.py'],
    pathex=[],
    binaries=all_binaries,
    datas=all_datas,
    hiddenimports=all_hiddenimports,
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[],
    noarchive=False,
    optimize=0,
)
pyz = PYZ(a.pure)

exe = EXE(
    pyz,
    a.scripts,
    [],
    exclude_binaries=True,
    name='bridge',
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    console=True,
    disable_windowed_traceback=False,
    argv_emulation=False,
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
)
coll = COLLECT(
    exe,
    a.binaries,
    a.datas,
    strip=False,
    upx=True,
    upx_exclude=[],
    name='bridge',
)
