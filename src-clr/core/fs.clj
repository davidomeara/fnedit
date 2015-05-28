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

(defn folder-browser-dialog [folder wrapper]
  (try
    (let [dialog (FolderBrowserDialog.)]
      (if (= (.ShowDialog dialog (.FindForm (:browser wrapper))) DialogResult/OK)
        (assoc folder :path (.SelectedPath dialog))
        (assoc folder :cancel true)))
    (catch Exception e (assoc folder :exception (.get_Message e)))))

(defn update-last-write-time [reload-file _]
  (try
    (if (File/Exists (:path reload-file))
      (assoc reload-file :last-write-time
                         (File/GetLastWriteTimeUtc (:path reload-file)))
      nil)
    (catch Exception e
      (assoc reload-file :last-write-time nil))))

(defn read-all-text [open-file _]
  (let [path (:path open-file)]
    (if path
      (try
        (-> open-file
            (assoc :text (File/ReadAllText path))
            (assoc :last-write-time (File/GetLastWriteTimeUtc path)))
        (catch Exception e (assoc open-file :exception (.get_Message e))))
      open-file)))

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

(defn save [m _]
  (let [{:keys [path text]} m]
    (if (and path text)
      (try
        (File/WriteAllText path text)
        (assoc m :last-write-time
          (File/GetLastWriteTimeUtc path))
        (catch Exception e
          (assoc m :exception (.get_Message e))))
      m)))

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
