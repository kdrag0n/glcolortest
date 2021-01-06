package dev.kdrag0n.blurtest

import com.topjohnwu.superuser.Shell

fun systemBoost(cb: () -> Unit = {}) {
    // Lock GPU and CPU frequencies, affine to prime cluster, freeze power HAL
    Shell.su(
        "settings put global airplane_mode_on 1",
        "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true",
        """
            for p in $(pm list packages --user 0 -3 | grep -v dev.kdrag0n.blurtest | sed 's/package://g')
            do
                pm suspend --user 0 ${'$'}p
                am force-stop --user 0 ${'$'}p
            done
        """.trimIndent(),
        "sleep 1",
        """
            for p in $(ps -A | grep -e com.google.android.gms -e libperfmgr | awk '{print ${'$'}2}')
            do
                echo "${'$'}p" > /sys/fs/cgroup/freezer/cgroup.procs
            done
        """.trimIndent(),
        "sleep 0.25",
        "echo 1 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel",
        "echo 1 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel",
        "echo 1 > /sys/class/kgsl/kgsl-3d0/force_rail_on",
        "echo 1 > /sys/class/kgsl/kgsl-3d0/force_clk_on",
        "echo 1 > /sys/class/kgsl/kgsl-3d0/force_bus_on",
        "echo 20000 > /sys/class/kgsl/kgsl-3d0/idle_timer",
        "echo 500000000 > /sys/class/kgsl/kgsl-3d0/max_gpuclk",
        "echo 500000000 > /sys/class/devfreq/3d00000.qcom,kgsl-3d0/min_freq",
        "echo 500000000 > /sys/class/devfreq/3d00000.qcom,kgsl-3d0/max_freq",
        "echo performance > /sys/devices/system/cpu/cpufreq/policy0/scaling_governor",
        "echo performance > /sys/devices/system/cpu/cpufreq/policy4/scaling_governor",
        "echo performance > /sys/devices/system/cpu/cpufreq/policy6/scaling_governor",
        "echo performance > /sys/devices/system/cpu/cpufreq/policy7/scaling_governor",
        "echo 50 > /dev/stune/top-app/schedtune.boost",
        "echo 1 > /dev/stune/top-app/schedtune.prefer_high_cap",
        "sleep 2",
        """
            main_pid="$(ps -A | grep dev.kdrag0n.blurtest | awk '{print ${'$'}2}')"
            for p in $(ls "/proc/${'$'}main_pid/task")
            do
                taskset -p 80 ${'$'}p
                chrt -fp ${'$'}p 10
            done
        """.trimIndent()
    ).submit { cb() }
}

fun systemUnboost(cb: () -> Unit = {}) {
    Shell.su(
        """
            for p in $(pm list packages --user 0 -3 | grep -v dev.kdrag0n.blurtest | sed 's/package://g')
            do
                pm unsuspend --user 0 ${'$'}p
            done
        """.trimIndent(),
        "echo 3 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel",
        "echo 0 > /sys/class/kgsl/kgsl-3d0/max_pwrlevel",
        "echo 0 > /sys/class/kgsl/kgsl-3d0/force_rail_on",
        "echo 0 > /sys/class/kgsl/kgsl-3d0/force_clk_on",
        "echo 0 > /sys/class/kgsl/kgsl-3d0/force_bus_on",
        "echo 80 > /sys/class/kgsl/kgsl-3d0/idle_timer",
        "echo 625000000 > /sys/class/kgsl/kgsl-3d0/max_gpuclk",
        "echo 275000000 > /sys/class/devfreq/3d00000.qcom,kgsl-3d0/min_freq",
        "echo 625000000 > /sys/class/devfreq/3d00000.qcom,kgsl-3d0/max_freq",
        "echo schedutil > /sys/devices/system/cpu/cpufreq/policy0/scaling_governor",
        "echo schedutil > /sys/devices/system/cpu/cpufreq/policy4/scaling_governor",
        "echo schedutil > /sys/devices/system/cpu/cpufreq/policy6/scaling_governor",
        "echo schedutil > /sys/devices/system/cpu/cpufreq/policy7/scaling_governor",
        "echo 10 > /dev/stune/top-app/schedtune.boost",
        "echo 0 > /dev/stune/top-app/schedtune.prefer_high_cap",
            """
            for p in $(ps -A | grep -e com.google.android.gms -e libperfmgr | awk '{print ${'$'}2}')
            do
                echo "${'$'}p" > /sys/fs/cgroup/cgroup.procs
            done
        """.trimIndent(),
        """
            main_pid="$(ps -A | grep dev.kdrag0n.blurtest | awk '{print ${'$'}2}')"
            for p in $(ls "/proc/${'$'}main_pid/task")
            do
                taskset -p ff ${'$'}p
                chrt -op ${'$'}p 0
            done
        """.trimIndent()
    ).submit { cb() }
}