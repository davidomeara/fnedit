(assembly-load-with-partial-name "System.Windows.Forms")

(ns core.fs
  (:import
   (System.IO
    Directory
    DirectoryInfo
    File
    FileStream
    FileMode
    Path)
   (System.Threading
    ApartmentState
    Thread
    ThreadStart)
   (System.Windows.Forms
    Form
    FolderBrowserDialog
    DialogResult)))

(defn folder-browser-dialog [wrapper]
  (try
    (let [dialog (FolderBrowserDialog.)]
      (if (= (.ShowDialog dialog (.FindForm (:browser wrapper))) DialogResult/OK)
        {:path (.SelectedPath dialog)}
        {:cancel true}))
    (catch Exception e {:exception (.get_Message e)})))

(defn last-write-time [path _]
  (try
    (if (File/Exists path)
      (File/GetLastWriteTimeUtc path)
      nil)
    (catch Exception e
      nil)))

(defn read-all-text [path _]
  (try
    {:text (File/ReadAllText path)
     :last-write-time (File/GetLastWriteTimeUtc path)}
    (catch Exception e {:exception (.get_Message e)})))

(defn combine-folder-path [m _]
  (let [folder-path (:folder-path m)
        file-name (:file-name m)]
    (if (and folder-path file-name)
      (try
        (assoc m :path (Path/Combine folder-path file-name))
        (catch Exception e (assoc m :exception (.get_Message e))))
      m)))

(defn create [m _]
  (let [path (:path m)]
    (if path
      (try
        (with-open [fs (FileStream. path FileMode/CreateNew)]
          (.Flush fs)
          m)
        (catch Exception e
          (-> m
              (dissoc :path)
              (assoc :exception (.get_Message e)))))
      m)))

(defn delete [m _]
  (let [path (:path m)]
    (if path
      (try
        (File/Delete path)
        m
        (catch Exception e
          (assoc m :exception (.get_Message e))))
      m)))

(defn save [path text _]
  (try
    (File/WriteAllText path text)
    [:last-write-time (File/GetLastWriteTimeUtc path)]
    (catch Exception e [:exception (.get_Message e)])))

(defn exists [path _]
  (try
    (File/Exists path)
    (catch Exception e false)))

(defn- node [path open]
  {:directories (->> (Directory/GetDirectories path)
                     (map (fn [p] [{:path p
                                    :name (-> p DirectoryInfo. .get_Name)}
                                   (when (contains? open p)
                                       (node p open))]))
                     (into (hash-map)))
   :files       (->> (Directory/GetFiles path)
                     (map (fn [p] {:path p
                                   :name (Path/GetFileName p)}))
                     (into (hash-set)))})

(defn remove-deleted-directory [open]
  (->> open
       (map #(when (Directory/Exists %) %))
       (remove nil?)
       (into (hash-set))))

(defn root-directory [path open _]
  (when path
    {{:path path
      :name (-> path DirectoryInfo. .get_Name)}
     (node path open)}))
