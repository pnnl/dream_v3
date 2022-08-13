<!-- Acknowledgements -->

<h1 style="margin-top:0in;margin-right:0in;margin-bottom:9.6pt;margin-left:0in;">Acknowledgments</h1>
<p style="margin-bottom:9.6pt;text-indent:.5in;">This work was completed as part of the National Risk Assessment Partnership (NRAP) project. Support for this project came from the U.S. Department of Energy&rsquo;s (DOE) Office of Fossil Energy&rsquo;s Crosscutting Research program.<span style="font-size:11px;"><a href="#_msocom_1" id="_anchor_1" language="JavaScript" name="_msoanchor_1">[AD1]</a>&nbsp;</span> The authors wish to acknowledge Mark McKoy (NETL Office of Science and Technology Strategic Plans and Programs), Darin Damiani (Carbon Storage Project Manager, and Mark Ackiewicz (Director of Division of Carbon Capture and Storage, DOE Office of Fossil Energy) for programmatic guidance, direction, and support. This work was supported by the Assistant Secretary for Fossil Energy, Office of Sequestration, Hydrogen, and Clean Coal Fuels, for the National Risk Assessment Partnership (NRAP) project managed by the National Energy Technology Laboratory (NETL). This work was performed under the auspices of the U.S. DOE by Pacific Northwest National Laboratory, operated by Battelle for the U.S. DOE under Contract DE-AC05-76RLO1830. &nbsp;Work at Lawrence Berkeley National Laboratory was completed under the U.S. DOE Contract No. DE-AC02-05CH1123. Work at Los Alamos National Laboratory was supported under the U.S. DOE Contract No. 89233218CNA000001.<span style='font-size:18px;font-family:"Georgia",serif;color:#2E2E2E;'>&nbsp;Work at&nbsp;</span>Lawrence Livermore National Laboratory was supported under the U.S. DOE Contract No. <a href="https://www.sciencedirect.com/science/article/pii/S175058362200007X#gs00004"><span style="color:windowtext;text-decoration:none;">DE-AC52-07NA27344</span></a> . Work at Los Alamos National Laboratory was supported under the U.S. DOE Contract No. <a href="https://www.sciencedirect.com/science/article/pii/S175058362200007X#gs00002"><span style="color:windowtext;text-decoration:none;">89233218CNA000001</span></a>.&nbsp;</p>

<div class="pagebreak"><br></div>
      
      
      
      
<!-- Abstract -->
      
<h1 style="margin-top:0in;margin-right:0in;margin-bottom:9.6pt;margin-left:0in;">ABSTRACT</h1>
<p style="margin-bottom:9.6pt;text-indent:.25in;">This user manual provides a brief guide for use of the <em>Designs for Risk Evaluation and Management</em> (<em>DREAM</em>) tool Version 3.0, developed as part of the effort to quantify the risk of geologic storage of carbon dioxide (CO<sub>2</sub>) under the U.S. Department of Energy&rsquo;s (DOE) National Risk Assessment Partnership (NRAP). DREAM is an optimization tool created to identify optimal monitoring schemes that minimize the time to first detection of unintended CO<sub>2</sub> leakage from a subsurface storage formation. This user manual illustrates the program and graphical user interface (GUI), provides guidance on how to define the inputs required, and includes an example application for the user to follow.</p>
<p style="margin-bottom:9.6pt;text-indent:.25in;">DREAM optimizes across user-provided outputs from subsurface leakage simulations or uses outputs from reduced order models (ROM). While DREAM was developed for CO<sub>2</sub> leakage scenarios, it is applicable to any subsurface leakage simulation of the same output format.</p>
<p style="margin-bottom:9.6pt;text-indent:.25in;">The DREAM tool is comprised of the four main components that are consecutively listed:</p>
<p style="margin-bottom:9.6pt;text-indent:.25in;">(1) a Java wizard used to configure and execute the simulations,</p>
<p style="margin-bottom:9.6pt;text-indent:.25in;">(2) a visualization tool to view the domain space and optimization results,</p>
<p style="margin-bottom:9.6pt;text-indent:.25in;">(3) plotting outputs to visually analyze the results,</p>
<p style="margin-bottom:9.6pt;text-indent:.25in;">(4) a Java application to aid users in converting common American Standard Code for Information Interchange (ASCII) output data to the standard DREAM hierarchical data format (HDF5).</p>
<p><span style='font-size:16px;font-family:"Times New Roman",serif;'>DREAM optimizes across user-provided subsurface leakage simulations based on user-specified leak parameters, factoring multiple objectives including minimizing cost, the earliest time to detection, and the volume of aquifer degraded. Currently, DREAM employs multi-objective optimization schemes that the user can select, including (i) Monte Carlo, (ii) Simulated Annealing, and (iii) Heuristic algorithm. The optimization schemes (i)-(iii) search for the optimal monitoring campaigns that minimize the tradeoff between the objectives, such as time to detection versus cost. One such approach, simulated annealing, searches the solution space by iteratively mutating potential monitoring schemes built off various configurations of monitoring locations and leak detection parameters. &nbsp;</span><span style='font-size:16px;font-family:"Times New Roman",serif;'>This approach has proven to be orders of magnitude faster than an exhaustive search of the entire solution space (Yonkofski et al., 2016).&nbsp;</span></p>
    
<div class="pagebreak"><br></div>





<!-- Introduction -->

<h1><a name="_Toc108614991"></a>1.&nbsp;&nbsp;&nbsp;&nbsp; Introduction</h1>
<p style="margin-bottom:9.6pt;text-align:justify;text-indent:.25in;">The <em>Designs for Risk Evaluation and Management</em> (<em>DREAM</em>) tool was developed at Pacific Northwest National Laboratory (PNNL) to guide the optimal placement of monitoring devices to detect carbon dioxide (CO<sub>2</sub>) leakage from storage formations. The National Risk Assessment Partnership (NRAP) identified the need for a user-friendly tool with the ability to design site-specific, risk-based monitoring strategies. NRAP is<span style="color:black;">&nbsp;a U.S. Department of Energy (DOE) project tasked with conducting risk and uncertainty analysis in the areas of reservoir performance, natural leakage pathways, wellbore integrity, groundwater protection, monitoring, and systems level modeling. Monitoring designs produced by DREAM may be&nbsp;</span>used by stakeholders and regulators to assist in compliance with regulatory requirements developed to ensure the safety of U.S. underground sources of drinking water (USDW) and ultimately lead to safe, permanent geologic CO<sub>2</sub> storage. Further, site-specific designs generated by <span style="color:black;">DREAM allow for&nbsp;</span>potential generalizations to other sites, as well as comparisons between risk-based monitoring designs and monitoring designs for other purposes, if such designs should already exist (e.g., from a Regional Carbon Sequestration Partnership [RCSP] site).</p>
<p style="margin-bottom:9.6pt;text-align:justify;text-indent:.25in;">DREAM optimizes across user-provided subsurface leakage simulations based on user-specified leak parameters, factoring multiple objectives including minimizing cost, the earliest time to detection, or the volume of aquifer degraded. Currently, DREAM employs multi-objective optimization schemes that the user can select, including (i) Monte Carlo, (ii) Simulated Annealing, and (iii) Heuristic algorithm. The optimization schemes (i)-(iii) search for the optimal monitoring campaigns that minimize the tradeoff between the objectives, such as time to detection versus cost. The default simulated annealing algorithm has proven to be orders of magnitude faster than an exhaustive search of the entire solution space, e.g., Grid Search (Yonkofski et al., 2016), and newer algorithms may prove to be even more efficient. Leak parameters may include pressure, temperature, gas saturation, dissolved component concentrations, pH, or any other quantity that can be modeled in a physics-based simulation of porous media fluid transport. DREAM allows for a mix of monitoring technologies including point sensors and surface measurements. While DREAM was designed with applications to CO<sub>2</sub> leakage in mind, this flexibility allows DREAM to determine optimal monitoring locations for any contaminant transport scenario<span style="font-size:11px;"><a href="#_msocom_1" id="_anchor_1" language="JavaScript" name="_msoanchor_1"></a></span>.</p>
<p style="margin-bottom:9.6pt;text-align:justify;text-indent:.25in;">Successful use of this software requires a rudimentary understanding of the leak scenarios, domain space, DREAM constraints, and the available optimization algorithms. There is rarely a definitively &ldquo;best&rdquo; monitoring solution, so DREAM instead functions as a support tool to highlight the opportunity trade-offs between good monitoring configurations. DREAM is limited by the spatial and temporal resolution of the subsurface leakage simulations, so decision-makers will need to translate DREAM configurations to real-world monitoring configurations that factor in site-specific constraints at a finer level. Decision-makers should always ground truth DREAM results to operational principles.</p>
<p style="margin-bottom:9.6pt;text-align:justify;text-indent:.25in;">Development of DREAM began in 2012 as part of the NRAP project, with the first code version being released in 2016, and the second major version released in 2020. The current version has been under development since then, and its release marks an important step in the completion of the second phase of the NRAP program. Subsequent versions are intended to add new objectives, monitoring technologies, algorithms, and input formats, while making continual improvements to user interface, post-processing analysis, and computational speeds.</p>
<p style="margin-bottom:9.6pt;text-indent:.25in;">This manual illustrates the program GUI and describes the tool inputs and outputs,<span style="font-size:11px;">&nbsp;&nbsp;</span>and how to interpret the results. For a synopsis of the theoretical basis of DREAM, see Yonkofski et al. (2016). For an example application using point source monitoring technologies from leakage simulations, see Yonkofski et al. (2017). For a combined application, see Bacon et al. (2019) for an example using NRAP-Open-IAM and DREAM to define a risk-based Post-Injection Site Care period and determine an optimized monitoring network for a commercial-scale CO2 storage project.</p>
<div id="_com_3" language="JavaScript"><br></div>




<!-- Software Installation and Requirements -->

<h1><a name="_Toc108614992"></a>2.&nbsp;&nbsp;&nbsp;&nbsp; Software Installation and Requirements</h1>
<p style="margin-bottom:9.6pt;text-align:justify;text-indent:.25in;">DREAM has three software dependencies that will support the effective use of the tool: Java, Python and HDF5 viewer.</p>
        </ol>
        <p style="margin-bottom:9.6pt;text-indent:.5in;"></p>
        <p style="margin-bottom:9.6pt;text-align:justify;"><strong>Java &ndash; Required</strong></p>
        <p style="margin-bottom:9.6pt;text-indent:.25in;">DREAM is coded almost exclusively in Java. Users must have the most recent release of the Java Platform, which is currently compatible with Version 8. If Java is already installed, a search for &ldquo;About Java&rdquo; will help find the current version on your local machine. The latest version of Java SE can be downloaded at the following link:</p>
        <p style="margin-bottom:9.6pt;"><a href="https://www.oracle.com/java/technologies/downloads/#java8">https://www.oracle.com/java/technologies/downloads/#java8</a></p>
        <p style="margin-bottom:9.6pt;">Note that you might have to create an account with Oracle before you can download the latest version of Java.</p>
        <p style="margin-bottom:9.6pt;text-align:justify;"><strong>Python 3 &ndash; Optional (strongly recommended)</strong></p>
        <p style="margin-bottom:9.6pt;text-indent:.25in;">The core DREAM functions will run without Python, but most of the post-processing scripts that provide valuable insights from results are created with Python. <span style='font-family:"Times",serif;'>If you already have Python installed, you can check your version by typing &ldquo;python --version&rdquo; into a command prompt. You can download the latest version of Python 3 at the following link:</span></p>
        <p style="margin-bottom:9.6pt;"><a href="https://www.python.org/downloads/">https://www.python.org/downloads/</a></p>
        <p style="margin-bottom:9.6pt;text-indent:.25in;">Python packages can also be installed by typing &ldquo;python -m pip install &lt;name&gt;&rdquo; into a command prompt. It is highly recommended to create a dedicated Python (or conda) environment for DREAM that will host compatible versions of the following required packages:</p>
        <ul style="list-style-type: disc;margin-left:0.25in;">
            <li><span style='font-size:16px;line-height:115%;font-family:"Times New Roman",serif;'>numpy</span><span style="font-size:11px;"><a href="#_msocom_4" id="_anchor_4" language="JavaScript" name="_msoanchor_4"></a></span></li>
            <li><span style='line-height:115%;font-family:"Times New Roman",serif;font-family:"Times New Roman",serif;font-size:12.0pt;'>scipy</span></li>
            <li><span style='line-height:115%;font-family:"Times New Roman",serif;font-family:"Times New Roman",serif;font-size:12.0pt;'>h5py</span></li>
            <li><span style='line-height:115%;font-family:"Times New Roman",serif;font-family:"Times New Roman",serif;font-size:12.0pt;'>matplotlib</span></li>
            <li><span style='line-height:115%;font-family:"Times New Roman",serif;font-family:"Times New Roman",serif;font-size:12.0pt;'>pandas</span><span style="line-height:115%;font-size:11px;">&nbsp;</span></li>
            <li><strong>HDF5 Viewer &ndash; Optional</strong></li>
        </ul>
        <p style="margin-bottom:9.6pt;text-align:justify;"></p>
        <p style="margin-bottom:9.6pt;text-indent:.25in;">DREAM converts raw leak simulation outputs into HDF5 files, a hierarchical data format that is designed to store and organize large amounts of data. The data can be explored with the <em>h5py</em> Python package that was mentioned earlier or with a packaged HDF5 viewer. DREAM developers recommend either installing HDFView or Panoply. The first application allows for simple viewing and editing of HDF5 files, while the second application allows for simple viewing and plotting. The viewers can be downloaded at the following links:</p>
        <p style="margin-bottom:9.6pt;">HDFView: <a href="https://www.hdfgroup.org/downloads/hdfview/">https://www.hdfgroup.org/downloads/hdfview/</a></p>
        <p style="margin-bottom:9.6pt;">Panoply: <a href="https://www.giss.nasa.gov/tools/panoply/download/">https://www.giss.nasa.gov/tools/panoply/download/</a></p>
        <p style="margin-bottom:9.6pt;text-indent:.25in;">The current release of DREAM is made available on the NETL Energy Data Exchange (EDX) at <span style="background:yellow;">XXXXX</span> and includes a zip file with the following files:</p>
        <ul style="list-style-type: disc;margin-left:0.25in;">
            <li><strong><span style='line-height:115%;font-family:"Times New Roman",serif;font-size:16px;'>A runnable JAR file</span></strong><span style='line-height:115%;font-family:"Times New Roman",serif;font-size:16px;'>, which packages all the necessary libraries, images, and documentation into an executable program.</span></li>
            <li><strong><span style='line-height:115%;font-family:"Times New Roman",serif;font-size:16px;'>Test Datasets</span></strong><span style="line-height:115%;font-size:11px;"><a href="#_msocom_9" id="_anchor_9" language="JavaScript" name="_msoanchor_9"></a></span><span style="line-height:115%;font-size:11px;"><a href="#_msocom_10" id="_anchor_10" language="JavaScript" name="_msoanchor_10"></a>&nbsp;</span><span style="font-size:16px;">that can be used by users to get familiar with the tool:</span>
                <ol style="list-style-type: circle;">
                    <li><u><span style="font-size:16px;">NRAP-OpenIAM Open Wellbore, Kimberlina site</span></u><span style="line-height:115%;font-size:11px;"><a href="#_msocom_12" id="_anchor_12" language="JavaScript" name="_msoanchor_12"></a>&nbsp;</span>
                        <ul class="decimal_type" style="list-style-type: square;">
                            <li><span style='line-height:115%;font-family:"Times New Roman",serif;font-size:16px;'>About the dataset: This dataset contains all the files</span><span style="line-height:115%;font-size:11px;"><a href="#_msocom_13" id="_anchor_13" language="JavaScript" name="_msoanchor_13"></a>&nbsp;</span><span style='line-height:115%;font-family:"Times New Roman",serif;font-size:16px;'>associated with the analysis of leakage risks at a brownfield site (i.e., Kimberlina site), using NRAP-Open-IAM.&nbsp;</span></li>
                            <li><span style='line-height:115%;font-family:"Times New Roman",serif;font-size:16px;'>Access:</span> <a href="https://edx.netl.doe.gov/dataset/application-of-nrap-open-iam-to-the-kimberlina-site"><span style="font-size:16px;">https://edx.netl.doe.gov/dataset/application-of-nrap-open-iam-to-the-kimberlina-site</span></a></li>
                            <li><span style='line-height:115%;font-family:"Times New Roman",serif;font-size:16px;'>Reference:</span> <span style='line-height:115%;font-family:"Times New Roman",serif;font-size:16px;'>Lackey G, VS Vasylkivska, NJ Huerta, S King, and RM Dilmore. 2019. &ldquo;Managing well leakage risks at a geologic carbon storage site with many wells.&rdquo; International Journal of Greenhouse Gas Control 88:182-194. 10.1016/j.ijggc.2019.06.011.</span></li>
                        </ul>
                    </li>
                    <li><u><span style="font-size:16px;">NRAP-OpenIAM Reservoir model<ins cite="mailto:Delphine%20Appriou" datetime="2022-03-16T16:16">:</ins></span></u></li>
                </ol>
            </li>
            <li><span style='font-size:16px;line-height:115%;font-family:"Times New Roman",serif;background:yellow;'><span class="msoIns"><ins cite="mailto:Delphine%20Appriou" datetime="2022-03-16T16:22">Provide description</ins></span></span><span style='font-size:16px;line-height:115%;font-family:"Times New Roman",serif;'><ins cite="mailto:Delphine%20Appriou" datetime="2022-03-16T16:31">:</ins></span><span style='font-size:11px;line-height:115%;font-family:"Times New Roman",serif;'>&nbsp;</span></li>
        </ul>
        <p style="margin-top:0in;margin-right:0in;margin-bottom:9.6pt;margin-left:1.25in;text-align:justify;"></p>
        <ol style="list-style-type: circle;">
            <li><u><span style="font-size:16px;">HDF5 files based on NUFT Simulations, Kimberlina 1.2 site</span></u></li>
            <li><span class="msoIns"><ins cite="mailto:Delphine%20Appriou" datetime="2022-03-16T16:29">About the dataset:&nbsp;</ins></span><span style='font-size:13px;font-family:"Calibri",sans-serif;'><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T08:48">Nonisothermal, Unsaturated Flow and Transport model</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T08:50">, referred to as&nbsp;</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T08:48">NUFT</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T08:50">, was developed by Los Alamos National Laboratory (LLNL) to&nbsp;</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T09:15">numerically&nbsp;</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T08:50">model</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T08:51">&nbsp;multi-phase non-isothermal flow and transport</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T08:52">&nbsp;for 3d simulations</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T08:54">&nbsp;of</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T08:58">&nbsp;</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T09:01">either the</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T08:54">&nbsp;saturated&nbsp;</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T09:01">zone&nbsp;</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T08:54">or unsaturated&nbsp;</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T08:59">zone</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T08:52">.&nbsp;</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T09:01">U</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T08:52">ser</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T09:01">s</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T08:52">&nbsp;</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T08:54">can&nbsp;</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T09:02">analyze</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T08:59">&nbsp;the</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T09:01">ir</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T08:59">&nbsp;site</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T09:01">s</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T08:59">&nbsp;of interest</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T10:01">&nbsp;for any leakage scenario</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T08:59">&nbsp;</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T09:02">using NUFT,&nbsp;</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T09:01">and then</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T09:03">&nbsp;they can</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T09:01">&nbsp;us</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T09:02">e these simulations as the inputs (e.g., hdf5 files) to run</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T08:54">&nbsp;</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T08:59">DREAM</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T09:02">.</ins><ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T08:51">&nbsp;</ins></span></li>
        </ol>
        <p style="margin-bottom:9.6pt;text-align:justify;"></p>
        <ul style="list-style-type: square;">
            <li><span style='line-height:115%;font-family:"Times New Roman",serif;font-size:16px;'><ins cite="mailto:Delphine%20Appriou" datetime="2022-03-16T16:29">&nbsp;</ins></span></li>
            <li><span style='line-height:115%;font-family:"Times New Roman",serif;font-size:16px;'><ins cite="mailto:Delphine%20Appriou" datetime="2022-03-16T16:29">Access:</ins></span><ins cite="mailto:Delphine%20Appriou" datetime="2022-03-16T16:31">https://edx.netl.doe.gov/dataset/llnl-kimberlina-1-2-nuft-simulations-june-2018-v2</ins><span style='line-height:115%;font-family:"Times New Roman",serif;font-family:"Times New Roman",serif;font-size:8.0pt;'><ins cite="mailto:Delphine%20Appriou" datetime="2022-03-16T16:33"><a href="#_msocom_15" id="_anchor_15" language="JavaScript" name="_msoanchor_15"></a></ins></span></li>
            <li><span style='line-height:115%;font-family:"Times New Roman",serif;font-size:16px;'><ins cite="mailto:Delphine%20Appriou" datetime="2022-03-16T16:29">Reference</ins><ins cite="mailto:Delphine%20Appriou" datetime="2022-03-16T16:46">s</ins><ins cite="mailto:Delphine%20Appriou" datetime="2022-03-16T16:29">:</ins><ins cite="mailto:Delphine%20Appriou" datetime="2022-03-16T16:30">&nbsp;</ins></span>
                <ul style="list-style-type: disc;">
                    <li><span style='line-height:115%;font-family:"Times New Roman",serif;font-size:16px;'><ins cite="mailto:Delphine%20Appriou" datetime="2022-03-16T16:30">Kayyum Mansoor, Thomas A. Buscheck, Xianjin Yang, Susan A. Carroll, Xiao Chen. LLNL Kimberlina 1.2 NUFT Simulations June 2018, 2018-06-25, https://edx.netl.doe.gov/dataset/llnl-kimberlina-1-2-nuft-simulations-june-2018, DOI: 10.18141/1603336</ins></span></li>
                    <li><span style='line-height:115%;font-family:"Times New Roman",serif;font-size:16px;'><ins cite="mailto:Delphine%20Appriou" datetime="2022-03-16T16:47">Yang, X., T. A. Buscheck, K. Mansoor, Z. Wang, K. Gao, L. Huang, D. Appriou, and S. A. Carroll (2019), Assessment of geophysical monitoring methods for detection of brine and CO2 leakage in drinking water aquifers, Int J Greenh Gas Con, 90, 102803.</ins></span></li>
                </ul>
            </li>
            <li><u><span style='font-size:16px;line-height:115%;font-family:"Times New Roman",serif;'><span style="text-decoration:none;">&nbsp;</span></span></u></li>
        </ul>
        <p style="margin-top:0in;margin-right:0in;margin-bottom:9.6pt;margin-left:1.25in;text-align:justify;"></p>
        <ol style="list-style-type: circle;">
            <li><u><span style="font-size:16px;">HDF5 files based on NUFT Simulations, Kimberlina 1.2 site (relocated, i.e., the leakage origin points are artificially randomized)</span></u><span style="line-height:115%;font-size:11px;">&nbsp;</span></li>
            <li><span style="background:yellow;"><ins cite="mailto:Delphine%20Appriou" datetime="2022-03-16T16:23">Provide description</ins></span></li>
        </ol>
        <p style="margin-top:0in;margin-right:0in;margin-bottom:9.6pt;margin-left:1.0in;text-align:justify;text-indent:.25in;"></p>
        <ol style="list-style-type: circle;">
            <li><u><span style="font-size:16px;">For more information on pre-processing data to run DREAM, please refer to Section&nbsp;</span></u><u><span style='line-height:115%;font-family:"Times New Roman",serif;font-size:16px;'>4</span></u><u><span style='line-height:115%;font-family:"Times New Roman",serif;font-size:16px;'>. This section will discuss the applications that can produce suitable data files needed to run DREAM, such as STOMP, NUFT, IAM, and TECPLOT. &nbsp;&nbsp;</span></u></li>
            <li><span style='line-height:115%;font-family:"Times New Roman",serif;font-family:"Times New Roman",serif;font-size:12.0pt;'>Examples of finished results to compare outputs to</span></li>
            <li><span style="font-family:CMR10;background:yellow;"><ins cite="mailto:Delphine%20Appriou" datetime="2022-03-16T16:24">Provide list</ins></span></li>
            <li>For any questions or feedback, please email <a href="mailto:alexander.hanna@pnnl.gov">alexander.hanna@pnnl.gov</a></li>
        </ol>
    </li>
</ol>
<div id="_com_3" language="JavaScript"><br></div>




<!-- User Interface Walkthrough -->

<h1><a name="_Toc108614992"></a>3.&nbsp;&nbsp;&nbsp;&nbsp; User Interface Walkthrough</h1>
<p style="margin-bottom:9.6pt;text-indent:.5in;">The DREAM tool is comprised of the four main components that are consecutively listed:</p>
<p style="margin-bottom:9.6pt;text-indent:.25in;">(1) a Java wizard used to configure and execute the simulations,</p>
<p style="margin-bottom:9.6pt;text-indent:.25in;">(2) a visualization tool to view the domain space and optimization results,&nbsp;</p>
<p style="margin-bottom:9.6pt;text-indent:.25in;">(3) plotting scripts used to analyze the results, &nbsp;</p>
<p style="margin-bottom:9.6pt;text-indent:.25in;">(4) a Java application to aid users in converting common American Standard Code for Information Interchange (ASCII) output data to the standard DREAM hierarchical data format (HDF5).</p>
<p style="margin-bottom:9.6pt;text-indent:.25in;">The example presented in this user manual is based on the test dataset from the brownfield site at Kimberlina Site, California, which is called Kimberlina 1.2. The data files are based on NUFT simulations at the Kimberlina site to model reactive multi-phase flow and transport of CO2 and brine for a case study of geologic carbon storage. In this tutorial, the user will be guided through the DREAM Java Wizard GUI while demonstrating an application to 19 randomly selected leakage scenarios generated for an NRAP Second-Generation Reduced-Order Model study (Carroll et al., 2014b). For context, a summary of the model set up from Carroll et al. (2014b) is provided below. <span style="font-size:11px;"><a href="#_msocom_4" id="_anchor_4" language="JavaScript" name="_msoanchor_4"></a></span></p>
<p style="margin-bottom:9.6pt;text-indent:.25in;">The Nonisothermal, Unsaturated Flow and Transport <ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T09:15">(</ins>NUFT<ins cite="mailto:Kupis,%20Shyla" datetime="2022-07-01T09:15">)</ins> numerical model<span style="font-size:11px;"><a href="#_msocom_5" id="_anchor_5" language="JavaScript" name="_msoanchor_5"></a>&nbsp;</span>for an alluvium case study of CO<sub>2</sub> storage (Figure 1) from the Kimberlina 1.2 site is comprised of a 3D heterogeneous domain that is represented as an unconsolidated aquifer consisting of permeable sand layers and impermeable shale layers that are based on the lithology of the High Plains Aquifer. The aquifer is underlain by a hypothetical CO<sub>2</sub> storage reservoir, and both aquifer and reservoir are penetrated by leaking wells. The model domain encompasses 10 km &times; 5 km &times; 240 m with 1 to 5 leakage sources per scenario placed at a depth of 198 m based on 48 known well locations. The wells are a mix of domestic, feedlot, irrigation, public water supply, and oil field water supply wells. Leakage rates are varied based on uncertainties in the hydrogeologic properties.</p>


<p style="margin-top:0in;margin-right:0in;margin-bottom:9.6pt;margin-left:0in;"> <span style="font-size: 9px"><b> Figure&nbsp;1: Beta Example Schematic. Figure from Carroll et al. (2014b) shows the links between reservoir, well leakage, and aquifer models using the alluvium case study. Links between reservoir, well leakage, and the carbonate case study are identical. </b></span></p>
<p style="margin-bottom:9.6pt;text-indent:.25in;">The files included in this tutorial contain output data <span style="font-size:11px;"></span>at specified times across all nodes representing hypothetical leakage scenarios from the CO<sub>2</sub> storage formation. DREAM will be used to optimize monitoring configurations that minimize the estimated time to first detection (TTD) of CO<sub>2</sub> leakage.</p>
<p style="margin-bottom:9.6pt;text-indent:.25in;">The DREAM GUI allows linear progression through a series of pages (Figure 2). The user can move back and forth but note that moving backwards may cause the user-inputs on later pages to be lost.</p>

<p style='margin-top:0in;margin-right:0in;margin-bottom:9.6pt;margin-left:0in;text-align:center;font-size:13px;font-family:"Times New Roman",serif;font-weight:bold;'><b>Figure 2: DREAM GUI Flow Chart</b></p>
<div id="_com_3" language="JavaScript"><br></div>



<h2 style="margin-top:0in;margin-right:0in;margin-bottom:9.6pt;margin-left:0in;">3.1 DREAM Welcome Page</h2>
<p style="margin-bottom:9.6pt;text-indent:.25in;">The DREAM<em>&nbsp;Welcome</em> page (Figure 3) provides links to the DREAM User Manual and literature detailing the technical development and theory behind the DREAM optimization algorithms<span style="font-size:11px;"><a href="#_msocom_1" id="_anchor_1" language="JavaScript" name="_msoanchor_1"></a>&nbsp;</span>(Yonkofski et al., 2016). The user is recommended to review the cited paper to thoroughly understand the objective function, decision variables and constraints, as well as the iterative process that DREAM performs to approach the optimal monitoring solution.</p>
<p style="margin-bottom:9.6pt;text-indent:.25in;">A conceptual figure on the right side of the <em>Welcome</em> window shows a theoretical DREAM application, with a leak from subsurface storage entering an overlying aquifer through an abandoned wellbore (Figure 3). While the user must provide the leakage scenario, this figurative example of inserts visualizes three of many monitoring configurations that DREAM can generate and assess during the iterative procedure. &nbsp;</p>

<p style='margin-top:0in;margin-right:0in;margin-bottom:9.6pt;margin-left:0in;text-align:center;font-size:13px;font-family:"Times New Roman",serif;font-weight:bold;'><b>Figure&nbsp;3: DREAM <em>Welcome</em> Page </b></p>
<p style='margin:0in;font-size:16px;font-family:"Times New Roman",serif;margin-top:0in;margin-right:0in;margin-bottom:9.6pt;margin-left:.25in;'>To continue to the example application, press <em>Next</em>.</p>
<div id="_com_3" language="JavaScript"><br></div>






<h2 style="margin-top:0in;margin-right:0in;margin-bottom:9.6pt;margin-left:0in;">3.2 Input Directory Page</h2>
<p style="margin-bottom:9.6pt;text-align:justify;text-indent:.25in;">The <em>Input Directory</em> page (Figure 4) prompts the user to select all files or the directory containing the files of the subsurface leakage simulations to be analyzed. There are three file types available: H5, IAM, and DREAM save files. H5 files are generated by selecting <em>Launch Converter</em> in the footer, which converts subsurface simulation outputs (e.g., NUFT, STOMP) to a structured file storage with a value for each parameter at each model node and time step. IAM files are generated to support Open-IAM, which simulates leaks as reduced order representations. H5 files are larger but provide more flexibility as the user can set their own leak and detection values. IAM files are smaller, representing only fixed leak and detection spaces. DREAM save files load a previous set of inputs</p>
<p style="margin-bottom:9.6pt;text-indent:.25in;">The HDF5, IAM or DREAM save files must be directly available within the input directory provided; they may not be in subdirectories. All scenarios within the directory should reference a single geographic location or results will not make sense. If the user wants to assess multiple locations, it should be done with different DREAM runs.</p>
<p style="margin-bottom:9.6pt;text-indent:.25in;">If the user has not converted ASCII simulation output data into DREAM readable HDF5 input files, the <em>Launch Converter</em> button will open a pop-up file converter tool. Read more about the DREAM HDF5 Converter tool in Section 4.</p>
<p style="margin-bottom:9.6pt;text-indent:.25in;">For this example, HDF5 files under &ldquo;root/inputs/kimberlina_1_2/&rdquo; will be used as input files. These input files are based on NUFT simulations for a case study on geologic carbon storage. Click <em>Select a directory</em> and navigate to the HDF5 files directory.</p>

<p style='margin-top:0in;margin-right:0in;margin-bottom:9.6pt;margin-left:0in;text-align:center;font-size:13px;font-family:"Times New Roman",serif;font-weight:bold;'><b>Figure&nbsp;4: <em>Input Directory</em> Page&nbsp;(by folders)</b></p>
<p style='margin-top:0in;margin-right:0in;margin-bottom:9.6pt;margin-left:0in;text-indent:.25in;font-size:16px;font-family:"Times New Roman",serif;'>Alternatively, use the &ldquo;Files&rdquo; option to select a particular assortment of input files from a directory (Figure 5). Click <em>Next.</em></p>

<p><a name="_Toc108615028"></a><b>Figure 5: <em>Input Directory</em> Page (by filenames)</b></p>
<p>A pop-up window (Figure 6) may prompt the user to specify any information missing from the HDF5 files, including porosity, units, and z-axis orientation, that will be specific to the model considered. Depending on units. If the HDF5 files are generated with the DREAM File Converter, they should contain all the necessary information and the user may never see this popup. IAM files will almost always ask for these inputs. The z-axis positive direction is important as depth of wells can factor into costs. Porosity is important as it factors into the volume of aquifer degraded calculations. Porosity can be entered in two ways: (1) as a variable in the HDF5 file that will allow porosity to vary across time and space or (2) as a constant scalar value that is applied to the whole domain either as an HDF5 attribute or entered in the pop-up window.</p>

<p><a name="_Toc108615029"></a><b>Figure 6: Pop-up to specify missing information</b></p>
<p>If the loaded HDF5 or IAM files do not include units for an input, then it must be specified. The possible units that you might be required to specify are:</p>
<ul>
<li>XYZ &ndash; meters or feet,</li>
<li>Z-Axis Positive Direction &ndash; up or down,</li>
<li>Time &ndash; years, months, or days,</li>
<li>Gravity units &ndash; mGal,</li>
<li>Pressure units &ndash; Pa,</li>
<li>Saturation &ndash; leave as unitless or add &ldquo;%&rdquo;, and</li>
<li>Set Porosity &ndash; insert as unitless value.</li>
</ul>
<div id="_com_3" language="JavaScript"><br></div>






<h2><a name="_Toc108614996"></a>3.3&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; Leak Definition</h2>
<p>The <em>Leak Definition </em>page (Figure 7) allows the user to define what will be considered as a leak by taking a union of leak parameters, such as saturation and/or gravity, to specify a leak definition for some user-specified criteria.</p>
<p>&nbsp;</p>
<p>&nbsp;Figure 7: Scenario <em>Leak Definition</em> Page</p>
<p>Under <em>Parameter</em>, pressure, saturation, and any other user-specified parameters can define a leak either individually or as a combination of parameters. Up to three parameters can be selected at a time to define a leak by taking the union of the selected parameters. Parameters are read in from the input H5 or IAM files and represent a value at each model node and timestep. The leak space is all the model nodes that exceed the defined type and value at any timestep. A carbon capture application might define the leak where CO2 saturation exceeds a maximum contaminant level.</p>
<p>Under <em>Leak Type</em>, there are three options that can be selected to define a leak:</p>
<ul>
<li>&ldquo;Absolute change&rdquo; and &ldquo;Relative change&rdquo; define leaks as the change across time and may be limited to positive or negative change by providing a +/- sign before the value.
<ul>
<li>&ldquo;Absolute change&rdquo; is all nodes that change from the original value by the specified value.</li>
<li>&ldquo;Relative change&rdquo; is all nodes that change relative to the original values as a percent. It should not start at 0 to avoid blowing up to infinity, e.g., relative change = (new value &ndash; ref value) / ref value</li>
</ul>
</li>
<li>&ldquo;Above threshold&rdquo; and &ldquo;Below threshold&rdquo; define leaks as falling above and below, respectively, of the threshold in units of the parameter. The leak space is all nodes exceeding or less than the value.</li>
</ul>
<p>The <em>Leak Type</em> can be set independently for each parameter and defines the type of change that is expected to signify a leak.</p>
<p>Hovering over the <em>Leak Value</em>will display the global minimum, average, and maximum for the parameter across all timesteps and scenarios to help the user make an informed selection. The leak value corresponds to the leak type and determines the leak space. The leak space is made of all model nodes that exceed the defined leak value at any scenario. This space determines how much aquifer has degraded at each timestep and scenario, indicating the size of the leak and potential environmental remediation costs.&nbsp; If multiple parameters are selected, the final leak space becomes the union of the parameters or nodes that meet any of the thresholds.</p>
<p>Most users may choose to select a CO<sub>2</sub> parameter exceeding some value for carbon capture applications; although, pressure or other proxies may be necessary if a CO<sub>2 </sub>parameter is not available. Clicking <em>Calculate Leak</em> will calculate which nodes exceed the threshold in at least one scenario, and the number of nodes found should The number of nodes in the leak space must be greater than 0 to continue.</p>
<p>and remove the default pressure leak type (Figure 8)<em>. Then,</em> click <em>Calculate Leak </em>and view how many nodes exceeded the saturation threshold. Finally, click <em>Next</em>.Figure 8: Calculating leak space on <em>Leak Definition</em> page</p>
<p>Click <em>Launch Visualization</em> to display the visualization of potential leak plumes captured by pressure, gravity, and saturation profiles when the leak threshold is exceeded at any time (Figure 9). The user may toggle between views and change which parameters to display and what the color and transparency of the parameter should be. The user may zoom in and out with the mouse or scale the grid with the &ldquo;Scale X/Y/Z&rdquo; sliders located in the top right pane. The <em>Monitoring Plan</em> configuration tab will become useful once the DREAM tool has run. Close out of the Visualization Tool and select <em>Next</em> on the <em>Leakage Definition</em> Window.</p>
<p><b>Figure 9. Interactive display of the leak plume after <em>Leak Definition</em></b></p>
<div id="_com_3" language="JavaScript"><br></div>





<h2>3.4&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; Scenario Weighting</h2>
<p>. This can be done with a simple weight equation that can use any combination of the following three variables:</p>
<ul>
<li><em>v </em>&ndash; maximum volume of aquifer degraded,</li>
<li><em>a </em>&ndash; user defined weights for <em>Variable A</em>, and</li>
<li><em>b</em> - user defined weights for <em>Variable B</em>.</li>
</ul>
<p>(1) The maximum volume of aquifer degraded <em>v</em> is automatically calculated for each scenario threshold (see Section 3.3) and by multiplying porosity by the total volume of nodes that exceed the threshold. Porosity is either a constant or can be set individually for each node with the input HDF5 file. The maximum value at any time step can be used as the variable <em>v</em> with the scenario weighting equation.</p>
<p>(2) For variables <em>a </em>and <em>b</em>, they can manually be set by the user or can be read in from a CSV file and used as a variable in the &ldquo;Weight Equation&rdquo;. They can represent variables, such as scenario likelihood, potential remediation cost, proximity to an area of concern, and so on. The default settings for the variables do not need to be changed if all scenarios will be weighted equally.</p>
<p>(3) Weights are relative, but the user can scale the w or normalize them to be between 0 and 1 for clarity. The user can also remove scenario weighting by selecting the button <em>Set Weights Equal</em>. The <em>Scale Weights</em> button will maintain the same relative weights while scaling the numbers from 0 to 1, which is recommended when weights are very large. Weighting is important in DREAM because it optimizes monitoring campaigns by testing random sensor placements and iterating towards &lsquo;better&rsquo; solutions based on objectives. Weighting scenarios can influence which monitoring campaign is preferred. DREAM can also factor scenario weighting in Pareto optimization to rank the top performing monitoring campaigns.</p>
<p>During multi-objective optimization in the last phase of running DREAM, theFor <em>Weight Equation</em>, it can be modified as needed using any of the variables: <em>v, a</em>, or <em>b</em>.</p>
<p>In this example, the default scheme of equal weighting is applied (Figure 10).</p>
<p><a name="_Toc108615033"></a><b>Figure 10: Default settings on <em>Scenario Weighting</em> page. </b></p>
<p>&nbsp;</p>
<h2><a name="_Toc108614998"></a>1.2&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; Detection Threshold</h2>
<p>The <em>Detection Threshold</em> page (Figure 11) allows the user to select monitoring technologies that are being considered based on the parameters available in the input files, and to define associated parameters, such as deployment cost and detection threshold.</p>
<p><a name="_Toc108615034"></a><b>Figure 11: Default settings on <em>Detection Threshold</em> page</p>
<p>This page has a lot of information that defines each technology, including:</b></p>
<ul>
<li>The <em>&lsquo;+&rsquo; box</em> allows technologies to be competed, creating a second monitoring parameter option that can have different cost, detection information, or zone limitations. It can be used to duplicate a <em>Parameter</em> if there are multiple sensor options (e.g., cheap vs. expensive).</li>
<li>The <em>check box</em> enables or disables a monitoring technology from the optimization.</li>
<li><em>Parameter</em> is where parameters are read in from the input H5 or IAM files and represent a value at each model node and timestep. Check all parameters that should be considered for monitoring.</li>
<li><em>Sensor Alias</em> allows the user to assign a recognizable name to the technology.</li>
<li><em>Cost</em> allows the user to assign a value to each monitoring technology unit. Cost can be entered as an equation that varies based on deployment method. It is a primary objective that can be assessed with the optimization algorithm.
<ul>
<li>For point sensors (e.g., pressure, saturation), &lsquo;<em>t</em>&rsquo; represents time that the sensor is deployed and &lsquo;<em>i</em>&rsquo; represents the number of installations.</li>
<li>For surface surveys (e.g., gravity survey), &lsquo;a&rsquo; represents the area being surveyed and &lsquo;s&rsquo; represents the number of after the original baseline.</li>
</ul>
</li>
<li><em>Detection Type</em> and <em>Detection Value</em> define the technical capabilities of the monitoring technology and the magnitude of detection that will reliably identify a leak. <em>Detection Type </em>can be set independently for each parameter and defines the type of change that a sensor is able to detect.
<ul>
<li>&ldquo;Absolute change&rdquo; means the sensor detect when a parameter exceeds the value. It has units of the <em>Monitoring Parameter</em>. &ldquo;Relative change&rdquo; is detected as percent change from the original value as a percent. Both work across time and may be limited to positive or negative change by providing a +/- sign before the value.</li>
<li>&ldquo;Above threshold&rdquo; and &ldquo;Below threshold&rdquo; (units of <em>Monitoring Parameter)</em> define detection as surpassing and falling below, respectively, the given threshold.</li>
</ul>
</li>
</ul>
<p>Hovering over the value input will display the global minimum, average, and maximum for the parameter across all timesteps and scenarios, helping the user make their selection.</p>
<ul>
<li><em>Deployment Method</em> categorizes the monitoring technology as either a point sensor or surface survey.
<ul>
<li><em>&ldquo;</em>Point Sensors&rdquo; are deployed at a single location within a well and continuously collect data to detect scalar changes in a parameter; although, they may be redeployed several times.</li>
<li>&ldquo;Surface Surveys&rdquo; cover a large area and have any number of discrete deployments while a survey is conducted. They involve periodic measurements taken by a survey crew to detect changes. It is assumed that a survey is conducted at time 0, and all future survey are detecting change from this time.</li>
</ul>
</li>
<li><em>Max Redeployments</em> determines the maximum number of times that a single point sensor can be moved, or the maximum number of times that a surface survey may be conducted. Surface surveys must be set to greater than 1 so that at least one reading is taken. It has a different meaning for different deployment methods.</li>
<li><em>Zone Bottom</em> and <em>Zone Top</em> define depth limitations to where the monitoring technology can be placed. By default, these values are set to the global minimum and maximum. The value is greyed out for surface surveys since they are conducted at the surface.</li>
</ul>
<p><b>Figure 12: Specifying criteria for each monitoring technology on Detection Threshold page </b></p>
<p>When loading IAM files, many user inputs are not available, as inputs are fixed during the process to generate IAM files. Some inputs may also be unavailable depending on the deployment method selected.</p>
<p>For this example, refer to Figure 12. First, select the <em>check box</em> for &ldquo;gravity&rdquo;, &ldquo;pressure&rdquo;, and &ldquo;saturation&rdquo;. Assign &ldquo;gravity&rdquo; with a cost equation of &ldquo;1500*s+250*a/1000000&rdquo; to represent $1500 per survey plus $250 per square kilometer of land surveyed. Gravity should also set <em>Detection Type</em> to &ldquo;Above threshold&rdquo;, <em>Detection Value</em> to 20 mGal, and <em>Max Redeployments</em> to 5. Pressure should set <em>Cost</em> to $500, <em>Detection Type</em> to &ldquo;Relative change&rdquo;, and <em>Detection Value</em> . Disclaimer: starting at 0 when calculating &ldquo;Relative change&rdquo; will cause an infinite value. Saturation should set <em>Cost</em> to $1500, <em>Detection Type</em> to &ldquo;Above threshold&rdquo;, and <em>Detection Value</em> to 2%. Finally, click <em>Find Detectable Nodes</em>. DREAM will calculate which nodes detect at least one scenario in the ensemble based on the user-defined detection type and value. The following values should appear next to each selected parameter type at the bottom of the page (Table 1).</p>

<p>Note: While the process is working, a red box appears to the right of the progress bar. Pressing this box cancels the process before completion but progress is saved.</p>
<p>&nbsp;</p>
<p><a name="_Toc108615062"></a>Table 1: Number of detectable nodes for each parameter in the example.</p>
<table>
<tbody>
<tr>
<td width="171">
<p><strong>Monitoring Parameter</strong></p>
</td>
<td width="180">
<p><strong>Detectable Plume</strong></p>
</td>
</tr>
<tr>
<td width="171">
<p>Gravity</p>
</td>
<td width="180">
<p>685 nodes</p>
</td>
</tr>
<tr>
<td width="171">
<p>Pressure</p>
</td>
<td width="180">
<p>19612 nodes</p>
</td>
</tr>
<tr>
<td width="171">
<p>Saturation</p>
</td>
<td width="180">
<p>3468 nodes</p>
</td>
</tr>
</tbody>
</table>
<p>&nbsp;</p>
<p>To visualize both the detectable plume and the leak plume, click <em>Launch Visualization </em>(Figure 13). When ready to continue, click <em>Next</em>.</p>
<p><a name="_Toc108615036"></a>Figure 13: Visualization of detectable plume</p>
<h2><a name="_Toc108614999"></a>1.1&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; Detection Criteria</h2>
<p>The <em>Detection Criteria</em> page (Figure 14) prompts the user to specify how many monitoring devices must be triggered to signify a leak has occurred. Only the sensors that were defined on the <em>Detection Threshold</em> page are selectable. &ldquo;Any Technology&rdquo; is a wildcard for any available sensors. Multiple criteria can be created by clicking the <em>Add a new criteria</em> button. Within a given test, the user may select any combination of specific technologies that will signify a leak. It is important to note that on this page we are taking all the instances where the nodes exceed the leak threshold from <em>Detection Threshold</em> (and <em>Leak Definition)</em> and are allowing the user to determine whether that instance qualifies as a &ldquo;leak&rdquo;, e.g., both pressure and saturation sensors exceed their leak detection thresholds.</p>
<p>Minimum Detections specifies how many detections from a specific sensor must exceed the user-defined value to have confidence that a leak occurred. Multiple tests with any combination of sensors can be created. To clarify, a full campaign of surface surveys monitoring an area is counted as one sensor, so it is advised not to require more than one surface survey. In most cases, it can be left at default, where one of any detecting sensors can identify a leak. For example, if both a cheap and expensive pressure sensor are available, two criteria can be created for one expensive or two cheap sensors need to be triggered to have a confidence in the leak.</p>
<p><a name="_Toc108615037"></a>Figure 14: Default settings for <em>Detection Criteria</em> page</p>
<p>For this example, set the first criteria, <em>Criteria 1</em>, to &ldquo;saturation&rdquo; with <em>Minimum Detections to Signify Leak </em>set to 1. Set the second criteria, <em>Criteria 2</em>, to &ldquo;gravity&rdquo; and &ldquo;pressure&rdquo; with <em>Minimum Detections to Signify Leak </em>set to 1 for each <em>Monitoring Technology </em>(Figure <strong>15</strong>). This implies confidence in a leak when either (1) a single saturation sensor exceeds the threshold or (2) both a gravity and pressure sensor exceed the threshold. Click <em>Next</em>.</p>
<p>&nbsp;</p>
<p><a name="_Toc108615038"></a>Figure <strong>15</strong>: Setting criteria for detecting a leak on <em>Detection Criteria</em> page</p>
